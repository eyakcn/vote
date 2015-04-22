package io.sophone.weixin;

import io.sophone.weixin.message.ArticleItem;
import io.sophone.weixin.message.OutgoingNewsMessage;
import net.sinofool.wechat.mp.WeChatMP;
import net.sinofool.wechat.mp.WeChatMPEventHandler;
import net.sinofool.wechat.mp.msg.*;
import net.sinofool.wechat.pay.WeChatPay;

import java.util.Arrays;

/**
 * @author eyakcn
 * @since 4/22/15 AD
 */
public class VertxWechatEventHandler implements WeChatMPEventHandler {
    private WeChatMP sdk;

    @Override
    public void setWeChatMP(WeChatMP mpSDK) {
        sdk = mpSDK;
    }

    @Override
    public void setWeChatPay(WeChatPay paySDK) {
        // TODO no need to use pay function currently
    }

    @Override
    public ReplyXMLFormat handle(IncomingTextMessage incoming) {
        OutgoingNewsMessage news = new OutgoingNewsMessage();
        news.setToUserName(incoming.getFromUserName());
        news.setFromUserName(incoming.getToUserName());

        if ("投票".equals(incoming.getContent())) {
            return wrapVoteNews(news);
        } else {
            // TODO other handling except voting
            return null;
        }
    }

    @Override
    public ReplyXMLFormat handle(IncomingSubscribeEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingSubscribeWithScanEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingScanEventMessage incoming) {
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingLocationEventMessage incoming) {
        return null;
    }

    @Override
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

    @Override
    public ReplyXMLFormat handle(IncomingViewEventMessage incoming) {
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
