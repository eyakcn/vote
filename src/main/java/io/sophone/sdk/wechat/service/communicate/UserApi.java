package io.sophone.sdk.wechat.service.communicate;

import com.google.gson.Gson;
import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;

import java.text.MessageFormat;
import java.util.function.BiConsumer;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public final class UserApi {
    private static final Logger logger = LoggerFactory.getLogger(UserApi.class);
    private static final String USRINFO_URL = "/cgi-bin/user/info?access_token={0}&openid={1}&lang=zh_CN";

    private final WechatConfig config;
    private final HttpClient http;
    private final BasicApi basicApi;

    public UserApi(WechatConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
        this.basicApi = new BasicApi(config, http);
    }

    public void fetchUserInfo(String openid, BiConsumer<User, String> userConsumer) {
        basicApi.getAccessToken(token -> {
            final String userUrl = MessageFormat.format(USRINFO_URL, token.access_token, openid);
            logger.info("Fetch user info: " + userUrl);
            HttpClientRequest userReq = http.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
                String line = userResBody.toString();
                User user = new Gson().fromJson(line, User.class);
                if (user.isSuccess()) {
                    userConsumer.accept(user, line);
                } else if (user.illegalOpenid()) {
                    userConsumer.accept(null, null);
                } else {
                    logger.error(user.errcode + ": " + user.errmsg);
                }
            }));
            userReq.exceptionHandler(t -> logger.error("Request to fetch user info failed.", t));
            userReq.end();
        });
    }

}
