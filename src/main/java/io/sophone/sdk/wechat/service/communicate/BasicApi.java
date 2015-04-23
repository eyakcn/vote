package io.sophone.sdk.wechat.service.communicate;

import com.google.gson.Gson;
import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public final class BasicApi {
    private static final Logger logger = LoggerFactory.getLogger(BasicApi.class);
    private static final String TOKEN_URL = "/cgi-bin/token?grant_type=client_credential&appid={0}&secret={1}";

    private final WechatConfig config;
    private final HttpClient http;

    private AccessToken currentToken;
    private long lastRefreshMillis;

    public BasicApi(WechatConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public void getAccessToken(Consumer<AccessToken> tokenConsumer) {
        if (!tokenExpired(currentToken)) {
            tokenConsumer.accept(currentToken);
            return;
        }

        currentToken = null;
        final String tokenUrl = MessageFormat.format(TOKEN_URL, config.getAppId(), config.getAppSecret());
        logger.info("Try to refresh access token. \n" + tokenUrl);
        HttpClientRequest request = http.get(tokenUrl, response -> response.bodyHandler(body -> {
            AccessToken token = new Gson().fromJson(body.toString(), AccessToken.class);
            if (token.isSuccess()) {
                currentToken = token;
                lastRefreshMillis = System.currentTimeMillis();
                logger.info("Access token refreshed: " + token.access_token);
                tokenConsumer.accept(token);
            } else {
                logger.error(token.errcode + ": " + token.errmsg);
            }
        }));
        request.exceptionHandler(t -> logger.error("Request to refresh token failed!", t));
        request.end();
    }

    private boolean tokenExpired(AccessToken token) {
        if (Objects.isNull(token)) {
            return true;
        }
        int deltaSeconds = (int) ((System.currentTimeMillis() - lastRefreshMillis) / 1000L);
        return deltaSeconds > token.expires_in - 5; // Refresh token 5 seconds before expires
    }
}
