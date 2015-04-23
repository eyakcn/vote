package io.sophone.sdk.wechat.model;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public final class AccessToken extends Status {
    private static final long serialVersionUID = 1L;

    public String access_token; // 获取到的凭证
    public int expires_in; // 凭证有效时间，单位：秒
    public long createTime; // 创建时间

    public AccessToken() {
        super();
    }

    public AccessToken(int errcode, String errmsg) {
        super(errcode, errmsg);
    }
}