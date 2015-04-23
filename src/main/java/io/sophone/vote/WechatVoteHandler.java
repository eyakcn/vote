package io.sophone.vote;

import com.google.gson.Gson;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import com.jetdrone.vertx.yoke.middleware.YokeResponse;
import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.OAuth2Token;
import io.sophone.sdk.wechat.model.User;
import io.sophone.sdk.wechat.service.communicate.UserApi;
import io.sophone.wechat.LocalConfig;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class WechatVoteHandler extends Middleware {
    private static final Logger logger = LoggerFactory.getLogger(WechatVoteHandler.class);

    private static final String TOKEN_URL = "/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
    private static final String USRINFO_URL = "/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";

    private static final String INDEX_HTML = "wechat/vote/index.html";
    private static final String DETAIL_HTML = "wechat/vote/detail.html";

    private final Map<String, Map<String, YokeRequest>> countingRequestsMap = new ConcurrentHashMap<>();
    private final WechatConfig config;
    private final HttpClient http;

    // SDK API
    private final UserApi userApi;

    public WechatVoteHandler(Verticle verticle) {
        config = new LocalConfig();
        http = verticle.getVertx().createHttpClient()
                .setPort(443) // XXX ssl only works on 443, not 80 port
                .setHost("api.weixin.qq.com")
                .setSSL(true)
                .setTrustAll(true)
                .setKeepAlive(true)
                .setMaxPoolSize(10);
        userApi = new UserApi(config, http);
    }

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        logger.info(request.ip() + " " + request.method() + " " + request.uri());
        switch (request.method()) {
            case "GET":
                String accept = request.getHeader("accept");
                String contentId = request.getParameter("content-id");
                if (Objects.equals("text/event-stream", accept)) {
                    // Counting request
                    request.exceptionHandler(event -> logger.error("Counting Request Exception", event));
                    request.response().exceptionHandler(event -> logger.error("Counting Response Exception", event));
                    String openid = request.getParameter("openid", request.ip());
                    if (Objects.nonNull(contentId)) {
                        Map<String, YokeRequest> countingRequests = countingRequestsMap.get(contentId);
                        if (Objects.isNull(countingRequests)) {
                            countingRequests = new ConcurrentHashMap<>();
                            countingRequestsMap.put(contentId, countingRequests);
                        }
                        countingRequests.put(openid, request);
                        logger.info("New counting reqeust for " + contentId + " from " + openid);
                    }
                } else if (Objects.isNull(contentId)) {
                    // index.html
                    request.exceptionHandler(event -> logger.error("Index Page Request Exception", event));
                    request.response().exceptionHandler(event -> logger.error("Index Page Response Exception", event));
                    handleGetIndex(request, next);
                } else {
                    // detail.html
                    request.exceptionHandler(event -> logger.error("Detail Page Request Exception", event));
                    request.response().exceptionHandler(event -> logger.error("Detail Page Response Exception", event));
                    handleGetContent(request, next);
                }
                break;
            case "PUT":
                break;
            case "POST":
                // submit voting request
                request.exceptionHandler(event -> logger.error("Vote Request Exception", event));
                request.response().exceptionHandler(event -> logger.error("Vote Response Exception", event));
                handlePost(request, next);
                break;
            case "DELETE":
                break;
            case "OPTIONS":
                break;
            case "HEAD":
                break;
            case "TRACE":
                break;
            case "PATCH":
                break;
            case "CONNECT":
                break;
        }
    }

    // FIXME no check on Wechat user based on openid
    private void handleGetIndex(YokeRequest request, Handler<Object> next) {
        List<VoteContent> contents = Context.getVoteContentList();
        request.put("contents", contents);

        String code = request.getParameter("code");
        logger.info("Handle request with code = " + code);
        // 服务号才能拿到授权访问用户信息，订阅号则不能，code用来换取Wechat服务器AccessToken，以进一步获取用户信息
        if (Objects.isNull(code)) {
            String paramOpenid = request.getParameter("openid");
            String userMapKey = request.getParameter("openid", request.ip());
            User user = Context.userMap.get(userMapKey);
            if (Objects.isNull(user)) {
                if (Objects.nonNull(paramOpenid)) {
                    userApi.fetchUserInfo(paramOpenid, userInfo -> {
                        if (Objects.nonNull(userInfo)) {
                            responseIndexPage(request, userInfo);
                            Context.recordUser(userInfo, null);
                        } else {
                            // openid does not exist
                            responseIndexPageWithIpUser(request);
                        }
                    });
                    return;
                } else {
                    // For non-wechat user, only IP address can be used
                    responseIndexPageWithIpUser(request);
                    return;
                }
            } else {
                responseIndexPage(request, user);
                return;
            }
        }

        final String tokenUrl = MessageFormat.format(TOKEN_URL, config.getAppId(), config.getAppSecret(), code);
        HttpClientRequest tokenReq = http.get(tokenUrl, tokenRes -> tokenRes.bodyHandler(tokenResBody -> {
            OAuth2Token auth = new Gson().fromJson(tokenResBody.toString(), OAuth2Token.class);
            if (Objects.isNull(auth.errcode) && StringUtils.isNotBlank(auth.openid)) {
                logger.info("Succeed to get access token by using redirect code.");

                User fetchedUser = Context.userMap.get(auth.openid);
                if (Objects.isNull(fetchedUser)) {
                    final String userUrl = MessageFormat.format(USRINFO_URL, auth.access_token, auth.openid);
                    HttpClientRequest userReq = http.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
                        User user = new Gson().fromJson(userResBody.toString(), User.class);
                        if (Objects.isNull(user.errcode)) {
                            responseIndexPage(request, user);

                            Context.recordUser(user, userResBody.toString());
                        } else {
                            logger.error(user.errcode + ": " + user.errmsg);
                        }
                    }));
                    userReq.end();
                } else {
                    responseIndexPage(request, fetchedUser);
                }
            } else {
                logger.error(auth.errcode + ": " + auth.errmsg);
            }
        }));
        tokenReq.end();
    }

    private void responseIndexPageWithIpUser(YokeRequest request) {
        User user = new User();
        user.openid = request.ip();
        user.ipBased = true;
        user.nickname = user.openid;
        responseIndexPage(request, user);

        // XXX is it ok to record IP user?
        Context.recordUser(user, null);
    }

    private void responseIndexPage(YokeRequest request, User user) {
        request.put("user", user);
        request.put("votedIds", Context.getVotedContentIds(user.openid));
        request.response().render(INDEX_HTML);
    }

    // FIXME Bug: the succeed submit followed by a failure submit
    private void refreshCounting(String contentId) {
        VoteContent voteContent = Context.getVoteContent(contentId);

        JsonObject countingResult = new JsonObject();
        countingResult.putNumber("total", voteContent.count);
        for (VoteCandidate candidate : voteContent.candidates) {
            countingResult.putNumber(candidate.caption, candidate.count);
        }

        String responseText = "data: " + countingResult.encode() + "\n\n";
        for (Map.Entry<String, YokeRequest> entry : countingRequestsMap.get(contentId).entrySet()) {
            String openid = entry.getKey();
            YokeRequest countingRequest = entry.getValue();
            writeSseMessage(countingRequest.response(), responseText);
            logger.info("Response counting request for " + contentId + " to " + openid);
        }
    }

    private void writeSseMessage(YokeResponse response, String line) {
        response.putHeader("Content-Type", "text/event-stream");
        try {
            response.putHeader("Content-Length", line.getBytes("UTF-8").length + "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        response.putHeader("Cache-Control", "no-cache");

        // TODO remove counting request from map when the connection is not alive
        try {
            response.write(line);
        } catch (Throwable t) {
            // May catch nothing, should set exception handler for response object
            logger.error("Counting Response Exception", t);
        }
    }

    private void handleGetContent(YokeRequest request, Handler<Object> next) {
        String contentId = request.getParameter("content-id");
        Objects.requireNonNull(contentId);

        VoteContent content = Context.getVoteContent(contentId);
        request.put("content", content);

        String openid = request.getParameter("openid", request.ip());
        User fetchedUser = Context.userMap.get(openid);
        if (Objects.isNull(fetchedUser)) {
            // XXX user skipped index page, and access detail page directly
            String url = "/wechat/vote";
            String paramOpenid = request.getParameter("openid");
            if (Objects.nonNull(paramOpenid)) {
                url += "?openid=" + paramOpenid;
            }
            request.response().redirect(url);
            return;
        }
        request.put("choices", Context.getVoteChoices(contentId, openid));

        boolean legalVoter = !(content.onlyWechat && fetchedUser.ipBased);
        boolean legalTime = true;
        long currentMilli = System.currentTimeMillis();
        if (!StringUtils.isBlank(content.startDate)) {
            long startMilli = getDateTimeMillis(content.startDate);
            legalTime = legalTime && (currentMilli >= startMilli);
        }
        if (legalTime && !StringUtils.isBlank(content.endDate)) {
            long endMilli = getDateTimeMillis(content.endDate);
            legalTime = legalTime && (currentMilli <= endMilli);
        }
        request.put("canVote", legalVoter && legalTime);

        // Warning message
        String message = null;
        if (!legalVoter) {
            message = "警告：只有微信用户可参与投票!";
        } else if (!legalTime) {
            message = "警告：投票时间 " + content.startDate + " ~ " + content.endDate;
        }
        if (Objects.nonNull(message)) {
            request.put("warning", message);
        }

        request.response().render(DETAIL_HTML);
    }

    private long getDateTimeMillis(String str) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("Asia/Shanghai"));
        return zonedDateTime.toInstant().toEpochMilli();
    }

    private void handlePost(YokeRequest request, Handler<Object> next) {
        String contentId = request.getParameter("content-id");
        Objects.requireNonNull(contentId);
        VoteContent content = Context.getVoteContent(contentId);

        JsonObject answer = request.<JsonObject>body();
        Objects.requireNonNull(answer);
        if (StringUtils.isBlank(answer.getString("openid"))) {
            if (content.onlyWechat) {
                next.handle(null);
                return;
            }
            answer.putString("openid", request.ip());
        }
        answer.putString("ip", request.ip());
        answer.putString("content-id", contentId);
        // TODO strict check about the voting time
        Context.analyzeAnswer(answer);

        String line = answer.encode();
        List<String> lines = new ArrayList<>();
        lines.add(line);
        File answerFile = new File(Context.answerFilePath);
        try {
            Files.write(answerFile.toPath(), lines, StandardOpenOption.APPEND);
            logger.info("New answer add to: " + Context.answerFilePath);
        } catch (IOException e) {
            logger.error("Failed to write answers file!" + e.toString());
        }
        request.response().end("true");

        // Server Send Event
        refreshCounting(contentId);
    }
}
