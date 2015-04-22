package io.sophone.weixin;

import net.sinofool.wechat.mp.WeChatMP;
import net.sinofool.wechat.mp.WeChatMPEventHandler;
import net.sinofool.wechat.mp.msg.*;
import net.sinofool.wechat.pay.WeChatPay;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class VertxWechatEventHandler implements WeChatMPEventHandler {
    private WeChatMP sdk;

    @Override
    public void setWeChatMP(WeChatMP mpSDK) {
        sdk = mpSDK;
    }

    @Override
    public void setWeChatPay(WeChatPay paySDK) {
        // TODO no need to use pay function currently
    }

    @Override
    public ReplyXMLFormat handle(IncomingTextMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingSubscribeEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingSubscribeWithScanEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingScanEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingLocationEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingClickEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingViewEventMessage incoming) {
        return null;
    }
}
