package io.sophone.weixin;

import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import com.jetdrone.vertx.yoke.middleware.YokeResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class WeixinHandler extends Middleware {
    private final HttpClient http;
    private final Logger logger;
    private final VertxWechat sdk;

    public WeixinHandler(Verticle verticle) {
        http = verticle.getVertx().createHttpClient()
                .setPort(443) // XXX ssl only works on 443, not 80 port
                .setHost("api.weixin.qq.com")
                .setSSL(true)
                .setTrustAll(true)
                .setKeepAlive(true)
                .setMaxPoolSize(20);
        logger = verticle.getContainer().logger();
        sdk = new VertxWechat(http, logger);
    }

    @Override
    public void handle(YokeRequest request, Handler<Object> next) {
        logger.info(request.method() + " request: " + request.uri());
        logger.info("From IP: " + request.ip());

        YokeResponse response = request.response();
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        try {
            switch (request.method()) {
                case "GET":
                    // validation request from wechat server
                    String echostr = request.getParameter("echostr", "");
                    logger.info("Echo String: " + echostr);
                    String validstr = sdk.validate(signature, echostr, Integer.valueOf(timestamp), nonce);
                    logger.info("Valid String: " + validstr);

                    response.setStatusCode(200);
                    response.putHeader("Content-Type", "text/plain");
                    response.putHeader("Content-Length", validstr.getBytes(StandardCharsets.UTF_8).length + "");
                    response.write(validstr);
                    response.end();
                    return;
                case "POST":
                    // user message sent from wechat server
                    String encryptType = request.getParameter("encrypt_type");
                    String msgSignature = request.getParameter("msg_signature");

                    String body = request.<Buffer>body().toString();
                    logger.info("Request Body: " + body);
                    String reply = sdk.incomingMessage(signature, Integer.valueOf(timestamp), nonce, encryptType, msgSignature, body);
                    if (Objects.nonNull(reply)) {
                        response.setStatusCode(200);
                        response.putHeader("Content-Type", "application/xml;charset=utf-8");
                        response.putHeader("Content-Length", reply.getBytes(StandardCharsets.UTF_8).length + "");
                        response.write(reply);
                        response.end();
                    } else {
                        // TODO the request not handled yet
                        response.setStatusCode(400);
                        response.setStatusMessage("Not supported yet.");
                        response.end();
                    }
                    return;
            }
        } catch (Throwable t) {
            response.setStatusCode(500);
            response.setStatusMessage(t.getMessage());
            response.end();
        }
    }
}
