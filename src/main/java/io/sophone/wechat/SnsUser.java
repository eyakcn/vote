package io.sophone.wechat;

import java.util.List;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class SnsUser {
    public String openid; // 用户的唯一标识
    public String nickname; // 用户昵称
    public Short sex; // 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
    public String province; // 用户个人资料填写的省份
    public String city; // 普通用户个人资料填写的城市
    public String country; // 国家，如中国为CN
    public String headimgurl; //用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空
    public List<String> privilege; // 用户特权信息，json 数组，如微信沃卡用户为（chinaunicom）

    public String errcode;
    public String errmsg;
}
