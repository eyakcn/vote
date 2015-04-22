package io.sophone.weixin;

import net.sinofool.wechat.mp.WeChatMP;
import net.sinofool.wechat.mp.WeChatMPConfig;
import net.sinofool.wechat.mp.WeChatMPEventHandler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.logging.Logger;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class VertxWechat {
    private final HttpClient http;
    private final Logger logger;
    private final WeChatMPConfig config = new VertxWechatConfig();
    private final WeChatMPEventHandler handler = new VertxWechatEventHandler();
    private final WeChatMP sdk;

    public VertxWechat(HttpClient http, Logger logger) {
        this.http = http;
        this.logger = logger;
        sdk = new WeChatMP(config, handler, null, null);
    }

    public String validate(final String signature, final String echostr, int timestamp, final String nonce) {
        return sdk.validate(signature, echostr, timestamp, nonce);
    }


    public String incomingMessage(
            final String signature, final int timestamp, final String nonce,
            final String encryptType, final String msgSignature, final String body) {
        return sdk.incomingMessage(signature, timestamp, nonce, encryptType, msgSignature, body);
    }
}
