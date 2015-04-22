package io.sophone.sdk.wechat.message.arch;

import io.sophone.sdk.wechat.model.ArticleItem;

import java.util.List;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public abstract class NewsMessage extends MessageBase {
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
