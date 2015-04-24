package io.sophone.sdk.wechat.model;

import java.util.List;
import java.util.Objects;

/**
 * Created by eyakcn on 2014/10/13.
 */
public final class User extends Status {
    public boolean ipBased = false; // the openid is based on the IP address
    public short subscribe; // 用户是否订阅该公众号标识，值为0时，代表此用户没有关注该公众号，拉取不到其余信息。
    public String openid; // 用户的唯一标识
    public String nickname; // 用户昵称
    public short sex; // 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
    public String province; // 用户个人资料填写的省份
    public String city; // 普通用户个人资料填写的城市
    public String country; // 国家，如中国为CN
    public String language; // 语言
    public String headimgurl; //用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空
    public List<String> privilege; // 用户特权信息，json 数组，如微信沃卡用户为（chinaunicom）
    public long subscribe_time; // 用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
    public String unionid;
    public String remark;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        if (openid == null && ((User) obj).openid == null) {
            return true;
        }
        return openid != null && openid.equals(((User) obj).openid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(openid);
    }

    public boolean illegalOpenid() {
        return errcode == 40003; // 不合法的OpenID，请开发者确认OpenID（该用户）是否已关注公众号，或是否是其他公众号的OpenID
    }
}
