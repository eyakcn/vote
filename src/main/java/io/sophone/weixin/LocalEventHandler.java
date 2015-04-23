package io.sophone.weixin;

import io.sophone.sdk.wechat.WechatApi;
import io.sophone.sdk.wechat.WechatEventHandler;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.IncomingClickEventMessage;
import io.sophone.sdk.wechat.message.impl.IncomingTextMessage;
import io.sophone.sdk.wechat.message.impl.OutgoingNewsMessage;
import io.sophone.sdk.wechat.model.ArticleItem;

import java.util.Arrays;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public class LocalEventHandler implements WechatEventHandler {
    private WechatApi sdk;

    public void setWeChatMP(WechatApi mpSDK) {
        sdk = mpSDK;
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
        voteArticle.setTitle("投票系统");
        voteArticle.setDescription("本系统不定期发布相关投票活动，点击图片开始。");
        voteArticle.setPicurl("http://www.comearly.com/webroot/vote.png");
        voteArticle.setUrl("http://www.comearly.com/wechat/vote?openid=" + news.getToUserName());
        news.setArticles(Arrays.asList(voteArticle));
        news.setCreateTime((int) (System.currentTimeMillis() / 1000));
        return news;
    }
}
