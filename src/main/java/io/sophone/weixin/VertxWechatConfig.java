package io.sophone.weixin;

import io.sophone.wechat.Config;
import net.sinofool.wechat.mp.WeChatMPConfig;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class VertxWechatConfig implements WeChatMPConfig {
    @Override
    public String getAppId() {
        return Config.wechatId.appid;
    }

    @Override
    public String getAppSecret() {
        return Config.wechatId.secret;
    }

    @Override
    public String getToken() {
        return Config.wechatId.token;
    }

    @Override
    public String getAESKey() {
        return Config.wechatId.aeskey;
    }

    @Override
    public String getOriginID() {
        return Config.wechatId.originid;
    }
}
