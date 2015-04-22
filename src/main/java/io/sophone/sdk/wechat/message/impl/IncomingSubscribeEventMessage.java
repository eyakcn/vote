package io.sophone.sdk.wechat.message.impl;

import io.sophone.sdk.wechat.message.arch.EventMessage;

public class IncomingSubscribeEventMessage extends EventMessage {

    private String event;

    @Override
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

}
