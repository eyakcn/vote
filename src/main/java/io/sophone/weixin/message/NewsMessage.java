package io.sophone.weixin.message;

import java.util.List;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class NewsMessage extends MessageBase {
    private List<ArticleItem> articles;

    public List<ArticleItem> getArticles() {
        return articles;
    }

    public void setArticles(List<ArticleItem> articles) {
        this.articles = articles;
    }

    @Override
    public String getMsgType() {
        return "news";
    }

}
