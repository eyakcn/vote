package io.sophone.weixin;

import net.sinofool.wechat.mp.WeChatMPHttpClient;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.logging.Logger;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class VertxWechatHttpClient implements WeChatMPHttpClient {
    private final HttpClient http;
    private final Logger logger;

    public VertxWechatHttpClient(HttpClient http, Logger logger) {
        this.http = http;
        this.logger = logger;
    }

    @Override
    public String get(String host, int port, String schema, String uri) {
        return null;
    }

    @Override
    public String post(String host, int port, String schema, String uri, String body) {
        return null;
    }
}
