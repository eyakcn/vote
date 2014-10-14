package io.sophone.vote;

import com.google.gson.Gson;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import io.sophone.wechat.Identity;
import io.sophone.wechat.OAuth2Token;
import io.sophone.wechat.SnsUser;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.text.MessageFormat;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class WechatVoteHandler extends Middleware {
    private static HttpClient httpClient;
    private static Identity id;
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

    public static void setIdentity(Identity identity) {
        id = identity;
    }

    public static void setContainer(Container container_) {
        container = container_;
    }

    private static final String TOKEN_URL = "/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
    private static final String USRINFO_URL = "/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";
    private static final VoteContent voteContent;

    static {
        voteContent = new VoteContent();
        voteContent.title = "最受欢迎实践队伍";
        voteContent.text = "经过华东政法大学第一届社会实践大赛初赛，18支优秀实践队伍脱颖而出进入决赛，同事角逐。。。";
        voteContent.count = 146;

        VoteCandidate candidate1 = new VoteCandidate();
        candidate1.caption = "A组1号";
        candidate1.text = "队伍简介：典当行业这个古老行业。。。";
        candidate1.count = 20;
        voteContent.candidates.add(candidate1);
        VoteCandidate candidate2 = new VoteCandidate();
        candidate2.caption = "A组2号";
        candidate2.text = "队伍简介：典当行业这个古老行业。。。";
        candidate2.count = 13;
        voteContent.candidates.add(candidate2);
        VoteCandidate candidate3 = new VoteCandidate();
        candidate3.caption = "A组3号";
        candidate3.text = "队伍简介：典当行业这个古老行业。。。";
        candidate3.count = 33;
        voteContent.candidates.add(candidate3);
        VoteCandidate candidate4 = new VoteCandidate();
        candidate4.caption = "A组4号";
        candidate4.text = "队伍简介：典当行业这个古老行业。。。";
        candidate4.count = 22;
        voteContent.candidates.add(candidate4);
    }

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        switch (request.method()) {
            case "GET":
                handleGet(request, next);
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

    private void handleGet(YokeRequest request, Handler<Object> next) {
        String code = request.getParameter("code");
//        String state = request.getParameter("state");
        if (code != null) {
            final String tokenUrl = MessageFormat.format(TOKEN_URL, id.appid, id.secret, code);
            HttpClientRequest tokenReq = httpClient.get(tokenUrl, tokenRes -> tokenRes.bodyHandler(tokenResBody -> {
                OAuth2Token token = new Gson().fromJson(tokenResBody.toString(), OAuth2Token.class);
                if (token.errcode == null) {
                    final String userUrl = MessageFormat.format(USRINFO_URL, token.access_token, token.openid);
                    HttpClientRequest userReq = httpClient.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
                        SnsUser user = new Gson().fromJson(userResBody.toString(), SnsUser.class);
                        if (user.errcode == null) {
                            request.put("user", user);
                            // TODO load vote content from file
                            request.put("content", voteContent);
                            request.response().render("vote.html");
                        }
                    }));
                    userReq.end();
                } else {
                    container.logger().error(token.errmsg);
                    request.response().end("Failed authentication.");
                }
            }));
            tokenReq.end();
        } else {
            request.put("user", new SnsUser());
            request.put("content", voteContent);
            request.response().render("vote.html");
        }
    }

    private void handlePost(YokeRequest request, Handler<Object> next) {
        if (request.body() == null) {
            return;
        }
        JsonObject answer = (JsonObject) request.body();
        String openid = answer.getString("openid");
        String title = answer.getString("title");
        Object[] selections = answer.getArray("selections").toArray();
    }
}
