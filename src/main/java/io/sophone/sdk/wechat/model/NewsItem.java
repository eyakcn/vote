package io.sophone.sdk.wechat.model;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public final class NewsItem {
    public String title; // 图文消息的标题
    public String thumb_media_id; // 图文消息的封面图片素材id（必须是永久mediaID）
    public short show_cover_pic; // 是否显示封面，0为false，即不显示，1为true，即显示
    public String author; // 作者
    public String digest; // 图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
    public String content; // 图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS
    public String url; // 图文页的URL
    public String content_source_url; // 图文消息的原文地址，即点击“阅读原文”后的URL
}
