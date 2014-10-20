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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class WechatVoteHandler extends Middleware {
    private static final String TOKEN_URL = "/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
    private static final String USRINFO_URL = "/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";
    private static final List<YokeRequest> countingRequests = new ArrayList<>();
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
                if ("text/event-stream".equals(accept)) {
                    countingRequests.add(request);
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

    private void refreshCounting() {
        JsonObject countingResult = new JsonObject();
        VoteContent voteContent = getVoteContent();
        VoteCounting voteCounting = Context.voteCountingMap.get(voteContent.title);
        countingResult.putNumber("total", voteCounting.usersCount());

        for (VoteCandidate candidate : voteContent.candidates) {
            countingResult.putNumber(candidate.caption, voteCounting.usersCountOf(candidate.caption));
        }

        String responseText = "data: " + countingResult.encode() + "\n\n";
        for (YokeRequest request : countingRequests) {
            writeSseMessage(request.response(), responseText);
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
        String code = request.getParameter("code");
//        String state = request.getParameter("state");

        VoteContent content = getVoteContent();
        request.put("content", content);

        if (code != null) {
            final String tokenUrl = MessageFormat.format(TOKEN_URL, Config.wechatId.appid, Config.wechatId.secret, code);
            HttpClientRequest tokenReq = httpClient.get(tokenUrl, tokenRes -> tokenRes.bodyHandler(tokenResBody -> {
                OAuth2Token token = new Gson().fromJson(tokenResBody.toString(), OAuth2Token.class);
                if (token.errcode == null && StringUtils.isNotBlank(token.openid)) {
                    SnsUser fetchedUser = Context.userMap.get(token.openid);
                    if (fetchedUser == null) {
                        final String userUrl = MessageFormat.format(USRINFO_URL, token.access_token, token.openid);
                        HttpClientRequest userReq = httpClient.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
                            SnsUser user = new Gson().fromJson(userResBody.toString(), SnsUser.class);
                            if (user.errcode == null) {
                                request.put("user", user);
                                request.response().render("vote.html");

                                Context.userMap.put(user.openid, user);
                                List<String> lines = new ArrayList<String>();
                                lines.add(userResBody.toString());
                                File userFile = new File(Context.userFilePath);
                                try {
                                    Files.write(userFile.toPath(), lines, StandardOpenOption.APPEND);
                                    container.logger().info("New user add to: " + Context.userFilePath);
                                } catch (IOException e) {
                                    container.logger().error("Failed to write user file!" + e.toString());
                                }
                            }
                        }));
                        userReq.end();
                    } else {
                        request.put("user", fetchedUser);
                        request.response().render("vote.html");
                    }
                } else {
                    container.logger().error(token.errmsg);
                    request.response().end("Failed authentication.");
                }
            }));
            tokenReq.end();
        } else {
            request.put("user", new SnsUser());
            request.response().render("vote.html");
        }
    }

    private VoteContent getVoteContent() {
        Collection<VoteContent> contents = Context.voteContentMap.values();
        VoteContent content = contents.isEmpty() ? new VoteContent() : contents.iterator().next();
        return content;
    }

    private void handlePost(YokeRequest request, Handler<Object> next) {
        if (request.body() == null) {
            return;
        }
        JsonObject answer = request.<JsonObject>body();
        if (StringUtils.isBlank(answer.getString("openid"))) {
            answer.putString("openid", request.ip());
        }
        Context.analyzeAnswer(answer);

        String line = answer.encode();
        List<String> lines = new ArrayList<String>();
        lines.add(line);
        File answerFile = new File(Context.answerFilePath);
        try {
            Files.write(answerFile.toPath(), lines, StandardOpenOption.APPEND);
            container.logger().info("New answer add to: " + Context.answerFilePath);
        } catch (IOException e) {
            container.logger().error("Failed to write answers file!" + e.toString());
        }
        request.response().end("true");
        refreshCounting();
    }
}
