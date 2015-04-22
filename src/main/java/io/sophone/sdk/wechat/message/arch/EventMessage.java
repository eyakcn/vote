package io.sophone.sdk.wechat.message.arch;

public abstract class EventMessage extends MessageBase {
    @Override
    public String getMsgType() {
        return "event";
    }

    public abstract String getEvent();
}
