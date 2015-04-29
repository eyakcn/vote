package io.sophone.sdk.wechat;

import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.*;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public interface WechatEventHandler {
    default ReplyXMLFormat handle(IncomingTextMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingSubscribeEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingSubscribeWithScanEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingScanEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingLocationEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingClickEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingViewEventMessage incoming) {
        return null;
    }

    default ReplyXMLFormat handle(IncomingMassSendJobFinishEventMessage incoming) {
        return null;
    }
}
