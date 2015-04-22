package io.sophone.sdk.wechat;

import io.sophone.sdk.wechat.message.arch.Message;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.*;
import org.dom4j.DocumentHelper;
import org.vertx.java.core.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class WechatAPI {
    private final WechatConfig config;
    private final WechatEventHandler eventHandler;
    private final Logger logger;
    private final byte[] appIdBytes;
    private final byte[] aesKeyBytes;

    public WechatAPI(WechatConfig config, WechatEventHandler eventHandler, Logger logger) {
        this.config = config;
        this.eventHandler = eventHandler;
        this.logger = logger;
        this.appIdBytes = config.getAppId().getBytes(Charset.forName("utf-8"));
        if (config.getAESKey() != null) {
            this.aesKeyBytes = DatatypeConverter.parseBase64Binary(config.getAESKey() + "=");
        } else {
            this.aesKeyBytes = null;
        }
    }

    public boolean isEncrypted() {
        return this.aesKeyBytes != null;
    }

    /**
     * Call this method when you have incoming validate request.<br>
     * It is usually GET request for your endpoint.
     *
     * @param signature
     * @param echostr
     * @param timestamp
     * @param nonce
     * @return
     */
    public String validate(final String signature, final String echostr, int timestamp, final String nonce) {
        return verify(signature, timestamp, nonce) ? echostr : "";
    }

    /**
     * Call this method when you have a incoming request.
     *
     * @param signature    From request query string 'signature'
     * @param timestamp    From request query string 'timestamp'
     * @param nonce        From request query string 'nonce'
     * @param encryptType  From request query string 'encrypt_type'
     * @param msgSignature From request query string 'msg_signature'
     * @param body         From request body
     * @return null if nothing to reply or something wrong. <br>
     * Application should always return empty page.
     * @throws WechatException When there is underlying exception thrown, <br>
     *                         Application should response error if wants WeChat platform
     *                         retry, or response empty page to ignore.
     */
    public String incomingMessage(final String signature, final int timestamp, final String nonce,
                                  final String encryptType, final String msgSignature, final String body) {
        if (!verify(signature, timestamp, nonce)) {
            logger.warn("Failed while verify signature of request query");
            return null;
        }

        String encMessage;
        if (isEncrypted()) {
            if (!encryptType.equals("aes")) {
                logger.warn("Supoort only encrypted account, please contact support for migration");
                return null;
            }

            encMessage = decryptMPMessage(verifyAndExtractEncryptedEnvelope(timestamp, nonce, msgSignature, body));
            if (encMessage == null) {
                logger.warn("Failed to extract encrypted envelope");
                return null;
            }
        } else {
            encMessage = body;
        }

        Message dec = Messages.parseIncoming(encMessage);
        if (dec == null) {
            logger.warn("Failed to decrypt message");
            return null;
        }
        ReplyXMLFormat rpl = dispatch(dec);
        if (rpl == null) {
            // This is normal situation, handler want.
            return null;
        }
        if (isEncrypted()) {
            String enc = encryptMPMessage(rpl.toReplyXMLString());
            if (enc == null) {
                logger.warn("Failed to encrypt message");
                return null;
            }
            return packAndSignEncryptedEnvelope(enc, WechatUtils.now(), WechatUtils.nonce());
        } else {
            return rpl.toReplyXMLString();
        }
    }

    private ReplyXMLFormat dispatch(Message dec) {
        if (dec instanceof IncomingTextMessage) {
            return eventHandler.handle((IncomingTextMessage) dec);
        } else if (dec instanceof IncomingSubscribeEventMessage) {
            return eventHandler.handle((IncomingSubscribeEventMessage) dec);
        } else if (dec instanceof IncomingSubscribeWithScanEventMessage) {
            return eventHandler.handle((IncomingSubscribeWithScanEventMessage) dec);
        } else if (dec instanceof IncomingScanEventMessage) {
            return eventHandler.handle((IncomingScanEventMessage) dec);
        } else if (dec instanceof IncomingLocationEventMessage) {
            return eventHandler.handle((IncomingLocationEventMessage) dec);
        } else if (dec instanceof IncomingClickEventMessage) {
            return eventHandler.handle((IncomingClickEventMessage) dec);
        } else if (dec instanceof IncomingViewEventMessage) {
            return eventHandler.handle((IncomingViewEventMessage) dec);
        } else {
            return null;
        }
    }

    private String packAndSignEncryptedEnvelope(String enc, int createTime, String nonce) {
        org.dom4j.Element root = DocumentHelper.createElement("xml");
        root.addElement("Encrypt").addCDATA(enc);
        root.addElement("MsgSignature").addCDATA(sign(createTime, nonce, enc));
        root.addElement("TimeStamp").addCDATA(createTime + "");
        root.addElement("Nonce").addCDATA(nonce);
        return root.asXML();
    }

    private String verifyAndExtractEncryptedEnvelope(final int timestamp, final String nonce,
                                                     final String msgSignature, final String body) {
        String encMessage = null;
        String toUserName = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document root = builder.parse(new ByteArrayInputStream(body.getBytes(Charset.forName("utf-8"))));
            Element doc = root.getDocumentElement();
            encMessage = doc.getElementsByTagName("Encrypt").item(0).getTextContent();
            toUserName = doc.getElementsByTagName("ToUserName").item(0).getTextContent();
        } catch (RuntimeException e) {
            logger.warn("Failed to parse XML:", e);
            throw new WechatException(e);
        } catch (Exception e) {
            logger.warn("Failed to parse XML", e);
            throw new WechatException(e);
        }

        if (!config.getOriginId().equals(toUserName)) {
            logger.warn("Failed to parse encrypted envelope, ToUserName expected=" +
                    config.getOriginId() + " not " + toUserName);
            return null;
        }

        if (!verify(msgSignature, timestamp, nonce, encMessage)) {
            logger.warn("Failed to verify encrypted envelope message signature.");
            return null;
        }

        return encMessage;
    }

    private boolean verify(final String signature, int timestamp, final String nonce) {
        String[] verify = new String[]{config.getToken(), String.valueOf(timestamp), nonce};
        Arrays.sort(verify);
        return signature.equals(WechatUtils.sha1hex(verify[0] + verify[1] + verify[2]));
    }

    private boolean verify(String signature, int timestamp, String nonce, String msg) {
        return signature.equals(sign(timestamp, nonce, msg));
    }

    private String sign(int timestamp, String nonce, String msg) {
        String[] verify = new String[]{config.getToken(), String.valueOf(timestamp), nonce, msg};
        Arrays.sort(verify);
        return WechatUtils.sha1hex(verify[0] + verify[1] + verify[2] + verify[3]);
    }

    final String decryptMPMessage(final String encMessage) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(aesKeyBytes, 0, 16);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] aesMsg = DatatypeConverter.parseBase64Binary(encMessage);
            byte[] msg = cipher.doFinal(aesMsg);
            int length = ((msg[16] & 0xFF) << 24) | ((msg[17] & 0xFF) << 16) | ((msg[18] & 0xFF) << 8)
                    | (msg[19] & 0xFF);
            if (20 + length + appIdBytes.length + msg[msg.length - 1] != msg.length) {
                logger.warn("decrypt message length not match length=" + length + ", msg.length=" + msg.length);
                return null;
            }
            for (int i = 0; i < appIdBytes.length; ++i) {
                if (appIdBytes[i] != msg[20 + length + i]) {
                    logger.warn("decrypt message appid not match " + config.getAppId() + " expected but " +
                            new String(msg, 20 + length, appIdBytes.length, Charset.forName("utf-8")) + " in message");
                    return null;
                }
            }
            return new String(msg, 20, length, Charset.forName("utf-8"));
        } catch (RuntimeException e) {
            logger.warn("Failed to decrypt message:", e);
            throw new WechatException(e);
        } catch (Exception e) {
            logger.warn("Failed to decrypt message", e);
            throw new WechatException(e);
        }
    }

    final String encryptMPMessage(final String rpl) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(aesKeyBytes, 0, 16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            byte[] messageBytes = rpl.getBytes(Charset.forName("utf-8"));
            int usefulLength = 20 + messageBytes.length + appIdBytes.length;
            int padLength = (usefulLength % 32 == 0) ? 32 : 32 - usefulLength % 32;

            byte[] buff = new byte[usefulLength + padLength];

            byte[] rand = new byte[16];
            WechatUtils.RAND.nextBytes(rand);
            for (int i = 0; i < 16; ++i) {
                buff[i] = rand[i];
            }

            buff[19] = (byte) (messageBytes.length & 0xFF);
            buff[18] = (byte) ((messageBytes.length >> 8) & 0xFF);
            buff[17] = (byte) ((messageBytes.length >> 16) & 0xFF);
            buff[16] = (byte) ((messageBytes.length >> 24) & 0xFF);

            for (int i = 0; i < messageBytes.length; ++i) {
                buff[i + 20] = messageBytes[i];
            }

            for (int i = 0; i < appIdBytes.length; ++i) {
                buff[i + 20 + messageBytes.length] = appIdBytes[i];
            }

            for (int i = 0; i < padLength; ++i) {
                buff[i + usefulLength] = (byte) padLength;
            }

            byte[] msg = cipher.doFinal(buff);
            String enc = DatatypeConverter.printBase64Binary(msg);
            return enc;
        } catch (RuntimeException e) {
            logger.warn("Failed to decrypt message:", e);
            throw new WechatException(e);
        } catch (Exception e) {
            logger.warn("Failed to encrypt message", e);
            throw new WechatException(e);
        }
    }

}
