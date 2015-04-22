package io.sophone.sdk.wechat.message.arch;

public abstract class TextMessage extends MessageBase {
    private String content;

    @Override
    public String getMsgType() {
        return "text";
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
