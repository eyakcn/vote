package io.sophone.sdk.wechat;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public interface WechatConfig {
    String getAppId();

    String getAppSecret();

    String getToken();

    String getAESKey();

    String getOriginId();
}
