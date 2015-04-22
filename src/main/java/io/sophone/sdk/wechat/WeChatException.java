package io.sophone.sdk.wechat;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public class WechatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WechatException(Exception e) {
        super(e);
    }

    public WechatException(String reason) {
        super(reason);
    }
}
