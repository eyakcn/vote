package io.sophone.sdk.wechat.message.impl;

import io.sophone.sdk.wechat.message.arch.TextMessage;

public class IncomingTextMessage extends TextMessage {

    private long msgId;

    public long getMsgId() {
        return this.msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }
}
