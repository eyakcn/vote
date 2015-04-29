package io.sophone.mass;

import io.sophone.sdk.wechat.WechatEventHandler;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.IncomingClickEventMessage;
import io.sophone.sdk.wechat.message.impl.IncomingMassSendJobFinishEventMessage;
import io.sophone.sdk.wechat.message.impl.OutgoingNewsMessage;
import io.sophone.sdk.wechat.model.ArticleItem;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public class ClassifyNewsHandler implements WechatEventHandler {
    private static final String baseDir = System.getProperty("user.home") + "/wechat/mass/";
    private static final String filePath = baseDir + "topic_filters.json";
    private static final List<TopicFilter> topicFilters;
    private static final Map<String, List<ArticleItem>> topicArticles = new ConcurrentHashMap<>();

    static {
        File filterFile = new File(filePath);
        try {
            if (filterFile.exists()) {
                byte[] bytes = Files.readAllBytes(filterFile.toPath());
                topicFilters = Json.decodeValue(new String(bytes, StandardCharsets.UTF_8), ArrayList.class);
            } else {
                topicFilters = new ArrayList<>();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReplyXMLFormat handle(IncomingMassSendJobFinishEventMessage incoming) {
        // TODO classify the latest news
        return null;
    }

    @Override
    public ReplyXMLFormat handle(IncomingClickEventMessage incoming) {
        OutgoingNewsMessage news = new OutgoingNewsMessage();
        news.setToUserName(incoming.getFromUserName());
        news.setFromUserName(incoming.getToUserName());
        news.setCreateTime((int) (System.currentTimeMillis() / 1000L));

        String eventKey = incoming.getEventKey();
        List<ArticleItem> articles = topicArticles.get(eventKey);
        news.setArticles(articles);
        return news;
    }
}
