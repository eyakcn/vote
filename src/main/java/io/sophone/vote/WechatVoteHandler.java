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

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        String code = request.getParameter("code");
        String state = request.getParameter("state");

        final String url = MessageFormat.format(TOKEN_URL, id.appid, id.secret, code);
        HttpClientRequest req = httpClient.get(url, res -> res.bodyHandler(body -> {
            OAuth2Token token = new Gson().fromJson(body.toString(), OAuth2Token.class);
            if (token.errcode == null) {
                final String url2 = MessageFormat.format(USRINFO_URL, token.access_token, token.openid);
                HttpClientRequest req2 = httpClient.get(url2, res2 -> res2.bodyHandler(body2 -> {
                    SnsUser user = new Gson().fromJson(body2.toString(), SnsUser.class);
                    if (user.errcode == null) {
                        request.put("user", user);
                        request.response().render("vote.html");
                    }
                }));
                req2.end();
            } else {
                container.logger().error(token.errmsg);
                request.response().end("Failed authentication.");
            }
        }));
        req.end();
    }
}
