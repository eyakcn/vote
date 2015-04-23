package io.sophone.sdk.wechat.model;

/**
 * Created by eyakcn on 2014/10/13.
 */
public final class OAuth2Token extends Status {
    public String access_token; // 网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
    public long expires_in; // access_token接口调用凭证超时时间，单位（秒）
    public String refresh_token; // 用户刷新access_token
    public String openid; // 用户唯一标识，请注意，在未关注公众号时，用户访问公众号的网页，也会产生一个用户和公众号唯一的OpenID
    public String scope; // 用户授权的作用域，使用逗号（,）分隔
    public String unionid; // 只有在用户将公众号绑定到微信开放平台账号后，才会出现该字段

    public OAuth2Token() {
        super();
    }

    public OAuth2Token(int errcode, String errmsg) {
        super(errcode, errmsg);
    }
}
