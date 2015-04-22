package io.sophone.sdk.wechat.message.arch;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public abstract class MessageBase implements Message {
    private String toUserName;
    private String fromUserName;
    private int createTime;

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }
}
