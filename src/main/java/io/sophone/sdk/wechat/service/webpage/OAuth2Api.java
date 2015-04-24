package io.sophone.sdk.wechat.service.webpage;

import com.google.gson.Gson;
import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.OAuth2Token;
import io.sophone.sdk.wechat.model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;

import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author eyakcn
 * @since 4/24/15 AD
 */
public class OAuth2Api {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Api.class);

    private static final String TOKEN_URL = "/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
    private static final String USRINFO_URL = "/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";

    private final WechatConfig config;
    private final HttpClient http;

    public OAuth2Api(WechatConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public void fetchUserInfo(OAuth2Token auth, BiConsumer<User, String> userConsumer) {
        final String userUrl = MessageFormat.format(USRINFO_URL, auth.access_token, auth.openid);
        HttpClientRequest userReq = http.get(userUrl, userRes -> userRes.bodyHandler(userResBody -> {
            final String line = userResBody.toString();
            final User user = new Gson().fromJson(line, User.class);
            if (user.isSuccess()) {
                userConsumer.accept(user, line);
            } else {
                logger.error(auth.errcode + ": " + auth.errmsg);
            }
        }));
        userReq.exceptionHandler(t -> logger.error("Request of fetch OAuth2 user failed.", t));
        userReq.end();
    }

    // TODO cache the access token
    public void getAccessToken(String code, Consumer<OAuth2Token> tokenConsumer) {
        final String tokenUrl = MessageFormat.format(TOKEN_URL, config.getAppId(), config.getAppSecret(), code);
        HttpClientRequest tokenReq = http.get(tokenUrl, tokenRes -> tokenRes.bodyHandler(tokenResBody -> {
            final OAuth2Token auth = new Gson().fromJson(tokenResBody.toString(), OAuth2Token.class);
            if (auth.isSuccess() && StringUtils.isNotBlank(auth.openid)) {
                logger.info("Succeed to get access token by using redirect code.");
                tokenConsumer.accept(auth);
            } else {
                logger.error(auth.errcode + ": " + auth.errmsg);
            }
        }));
        tokenReq.exceptionHandler(t -> logger.error("Request of fetch OAuth2 token failed.", t));
        tokenReq.end();
    }
}
