package io.sophone.mass;

import io.sophone.sdk.wechat.WechatEventHandler;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.IncomingClickEventMessage;
import io.sophone.sdk.wechat.message.impl.IncomingMassSendJobFinishEventMessage;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public class ClassifyNewsHandler implements WechatEventHandler {

    @Override
    public ReplyXMLFormat handle(IncomingMassSendJobFinishEventMessage incoming) {
        // TODO classify the latest news
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingClickEventMessage incoming) {
        // TODO show the classified news
        return null;
    }
}
