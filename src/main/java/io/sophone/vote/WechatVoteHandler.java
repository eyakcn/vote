package io.sophone.vote;

import com.google.gson.Gson;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import com.jetdrone.vertx.yoke.middleware.YokeResponse;
import io.sophone.wechat.Config;
import io.sophone.wechat.OAuth2Token;
import io.sophone.wechat.SnsUser;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class WechatVoteHandler extends Middleware {
    private static final String TOKEN_URL = "/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
    private static final String USRINFO_URL = "/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";

    private static final String INDEX_HTML = "wechat/vote/index.html";
    private static final String DETAIL_HTML = "wechat/vote/detail.html";

    private static final Map<String, Map<String, YokeRequest>> countingRequestsMap = new HashMap<>();
    private static HttpClient httpClient;
    private static Container container;

    public static void setHttpClient(@NotNull HttpClient httpClient_) {
        httpClient = httpClient_;
        httpClient.setPort(443) // XXX ssl only works on 443, not 80 port
                .setHost("api.weixin.qq.com")
                .setSSL(true)
                .setTrustAll(true)
                .setKeepAlive(true)
                .setMaxPoolSize(10);
    }

    public static void setContainer(Container container_) {
        container = container_;
    }

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        switch (request.method()) {
            case "GET":
                String accept = request.getHeader("accept");
                String contentId = request.getParameter("content-id");
                if (Objects.equals("text/event-stream", accept)) {
                    String openid = request.getParameter("openid", request.ip());
                    if (Objects.nonNull(contentId)) {
                        Map<String, YokeRequest> countingRequests = countingRequestsMap.get(contentId);
                        if (Objects.isNull(countingRequests)) {
                            countingRequests = new HashMap<>();
                            countingRequestsMap.put(contentId, countingRequests);
                        }
                        countingRequests.put(openid, request);
                        container.logger().info("New counting reqeust from : " + openid);
                    }
                } else if (Objects.isNull(contentId)) {
                    handleGetIndex(request, next);
                } else {
                    handleGetContent(request, next);
                }
                break;
            case "PUT":
                break;
            case "POST":
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

    private void handleGetIndex(YokeRequest request, Handler<Object> next) {
        List<VoteContent> contents = Context.getVoteContentList();
        request.put("contents", contents);

        String code = request.getParameter("code");
        if (Objects.isNull(code)) {
            // Request is not from Wechat
            SnsUser user = new SnsUser();
            user.nickname = request.ip();
            request.put("user", user);
            request.put("votedIds", getVotedContentIds(request.ip()));
            request.response().render(INDEX_HTML);
        }

        final String tokenUrl = MessageFormat.format(TOKEN_URL, Config.wechatId.appid, Config.wechatId.secret, code);
        HttpClientRequest tokenReq = httpClient.get(tokenUrl, tokenRes -> tokenRes.bodyHandler(tokenResBody -> {
            OAuth2Token auth = new Gson().fromJson(tokenResBody.toString(), OAuth2Token.class);
            if (Objects.isNull(auth.errcode) && StringUtils.isNotBlank(auth.openid)) {
                SnsUser fetchedUser = Context.userMap.get(auth.openid);
                if (fetchedUser == null) {
                    final String userUrl = MessageFormat.format(USRINFO_URL, auth.access_token, auth.openid);
                    HttpClientRequest userReq = httpClient.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
                        SnsUser user = new Gson().fromJson(userResBody.toString(), SnsUser.class);
                        if (Objects.isNull(user.errcode)) {
                            request.put("user", user);
                            request.put("votedIds", getVotedContentIds(user.openid));
                            request.response().render(INDEX_HTML);

                            Context.userMap.put(user.openid, user);
                            List<String> lines = new ArrayList<>();
                            lines.add(userResBody.toString());
                            File userFile = new File(Context.userFilePath);
                            try {
                                Files.write(userFile.toPath(), lines, StandardOpenOption.APPEND);
                                container.logger().info("New user add to: " + Context.userFilePath);
                            } catch (IOException e) {
                                container.logger().error("Failed to write user file!" + e.toString());
                            }
                        } else {
                            container.logger().error(user.errmsg);
                        }
                    }));
                    userReq.end();
                } else {
                    request.put("user", fetchedUser);
                    request.put("votedIds", getVotedContentIds(fetchedUser.openid));
                    request.response().render(INDEX_HTML);
                }
            } else {
                container.logger().error(auth.errmsg);
            }
        }));
        tokenReq.end();
    }

    private List<String> getVotedContentIds(String openid) {
        List<String> ids = Context.voteCountingMap.entrySet().stream()
                .filter(entry -> entry.getValue().alreadyVoted(openid))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
        return ids;
    }

    private void refreshCounting(String contentId) {
        VoteContent voteContent = Context.getVoteContent(contentId);

        JsonObject countingResult = new JsonObject();
        countingResult.putNumber("total", voteContent.count);
        for (VoteCandidate candidate : voteContent.candidates) {
            countingResult.putNumber(candidate.caption, candidate.count);
        }

        String responseText = "data: " + countingResult.encode() + "\n\n";
        for (Map.Entry<String, YokeRequest> entry : countingRequestsMap.get(contentId).entrySet()) {
            // TODO how to test whether connection is alive?
            writeSseMessage(entry.getValue().response(), responseText);
            container.logger().info("Response to counting request from : " + entry.getKey());
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
        response.write(line);
    }

    private void handleGetContent(YokeRequest request, Handler<Object> next) {
        String contentId = request.getParameter("content-id");
        Objects.requireNonNull(contentId);

        VoteContent content = Context.getVoteContent(contentId);
        request.put("content", content);

        String openid = request.getParameter("openid", request.ip());
        SnsUser fetchedUser = Context.userMap.get(openid);
        request.put("canVote", !content.onlyWechat || Objects.nonNull(fetchedUser));

        request.response().render(DETAIL_HTML);
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
        Context.analyzeAnswer(answer);

        String line = answer.encode();
        List<String> lines = new ArrayList<>();
        lines.add(line);
        File answerFile = new File(Context.answerFilePath);
        try {
            Files.write(answerFile.toPath(), lines, StandardOpenOption.APPEND);
            container.logger().info("New answer add to: " + Context.answerFilePath);
        } catch (IOException e) {
            container.logger().error("Failed to write answers file!" + e.toString());
        }
        request.response().end("true");

        // Server Send Event
        refreshCounting(contentId);
    }
}
