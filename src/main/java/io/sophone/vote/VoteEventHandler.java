package io.sophone.vote;

import io.sophone.sdk.wechat.WechatEventHandler;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.IncomingClickEventMessage;
import io.sophone.sdk.wechat.message.impl.IncomingTextMessage;
import io.sophone.sdk.wechat.message.impl.OutgoingNewsMessage;
import io.sophone.sdk.wechat.model.ArticleItem;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public class VoteEventHandler implements WechatEventHandler {
    private HttpServerRequest request;

    @Override
    public void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public ReplyXMLFormat handle(IncomingTextMessage incoming) {
        OutgoingNewsMessage news = new OutgoingNewsMessage();
        news.setToUserName(incoming.getFromUserName());
        news.setFromUserName(incoming.getToUserName());

        if ("投票".equals(incoming.getContent())) {
            return wrapVoteNews(news);
        } else {
            // other handling except voting
            return null;
        }
    }

    public ReplyXMLFormat handle(IncomingClickEventMessage incoming) {
        OutgoingNewsMessage news = new OutgoingNewsMessage();
        news.setToUserName(incoming.getFromUserName());
        news.setFromUserName(incoming.getToUserName());

        switch (incoming.getEventKey()) {
            case "VOTE_SYS":
                return wrapVoteNews(news);
        }
        return null;
    }

    private OutgoingNewsMessage wrapVoteNews(OutgoingNewsMessage news) {
        ArticleItem voteArticle = new ArticleItem();
        String domain = "www.comearly.com";
        if (Objects.nonNull(request)) {
            // TODO get domain name
        }
        voteArticle.setTitle("火热投票进行中！");
        voteArticle.setDescription(Context.voteTheme.theme);
        voteArticle.setPicurl("http://" + domain + "/sysroot/wechat/vote/image/" + Context.voteTheme.image);
        voteArticle.setUrl("http://" + domain + "/wechat/vote?openid=" + news.getToUserName());
        news.setArticles(Arrays.asList(voteArticle));
        news.setCreateTime((int) (System.currentTimeMillis() / 1000));
        return news;
    }
}
