package io.sophone.weixin.message;

import net.sinofool.wechat.mp.msg.ReplyXMLFormat;

import java.text.MessageFormat;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class OutgoingNewsMessage extends NewsMessage implements ReplyXMLFormat {
    private static final String NEWS_TEMPL = "" +
            "<xml>\n" +
            "  <ToUserName><![CDATA[{0}]]></ToUserName>\n" +
            "  <FromUserName><![CDATA[{1}]]></FromUserName>\n" +
            "  <CreateTime>{2}</CreateTime>\n" +
            "  <MsgType><![CDATA[news]]></MsgType>\n" +
            "  <ArticleCount>{3}</ArticleCount>\n" +
            "  <Articles>\n" +
            "{4}" +
            "  </Articles>\n" +
            "</xml> ";
    private static final String ARTICLE_TEMPL = "" +
            "    <item>\n" +
            "      <Title><![CDATA[{0}]]></Title> \n" +
            "      <Description><![CDATA[{1}]]></Description>\n" +
            "      <PicUrl><![CDATA[{2}]]></PicUrl>\n" +
            "      <Url><![CDATA[{3}]]></Url>\n" +
            "    </item>\n";


    @Override
    public String toReplyXMLString() {
        StringBuilder articleBuilder = new StringBuilder();
        for (ArticleItem item : getArticles()) {
            String article = MessageFormat.format(ARTICLE_TEMPL,
                    item.getTitle(), item.getDescription(), item.getPicurl(), item.getUrl());
            articleBuilder.append(article);
        }
        final String news = MessageFormat.format(NEWS_TEMPL,
                getToUserName(), getFromUserName(), getCreateTime(), getArticles().size(), articleBuilder.toString());
        return news;
    }
}
