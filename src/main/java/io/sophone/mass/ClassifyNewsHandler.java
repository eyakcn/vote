package io.sophone.mass;

import io.sophone.sdk.wechat.WechatEventHandler;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.impl.IncomingClickEventMessage;
import io.sophone.sdk.wechat.message.impl.IncomingMassSendJobFinishEventMessage;
import io.sophone.sdk.wechat.message.impl.OutgoingNewsMessage;
import io.sophone.sdk.wechat.model.ArticleItem;
import io.sophone.sdk.wechat.model.MaterialItem;
import io.sophone.sdk.wechat.model.NewsItem;
import io.sophone.sdk.wechat.service.communicate.MaterialApi;
import io.sophone.wechat.LocalConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ClassifyNewsHandler.class);

    private static final String baseDir = System.getProperty("user.home") + "/wechat/mass/";
    private static final String filePath = baseDir + "topic_filters.txt";
    private static final Map<String, List<ArticleItem>> topicArticles = new ConcurrentHashMap<>();
    // XXX make sure LocalConfig has been set with globalConfig and globalHttp
    private static final MaterialApi materialApi = new MaterialApi(LocalConfig.getGlobalConfig(), LocalConfig.getGlobalHttp());
    private static final List<TopicFilter> topicFilters = new ArrayList<>();

    static {
        loadTopicFilters();
        loadTopicArticles();
    }

    private static void loadTopicArticles() {
        materialApi.batchGetAll("news", ClassifyNewsHandler::filterTopicArticles);
    }

    private static void filterTopicArticles(List<MaterialItem> materialItems) {
        for (MaterialItem materialItem : materialItems) {
            List<NewsItem> items = materialItem.content.news_item;
            for (NewsItem item : items) {
                for (TopicFilter filter : topicFilters) {
                    if (item.title.contains(filter.getTopic())) {
                        if (!topicArticles.containsKey(filter.getEventKey())) {
                            List<ArticleItem> articles = new ArrayList<>();
                            topicArticles.put(filter.getEventKey(), articles);
                        }
                        List<ArticleItem> articles = topicArticles.get(filter.getEventKey());

                        ArticleItem article = new ArticleItem();
                        article.setTitle(item.title);
                        article.setDescription(item.content);
                        article.setUrl(item.url);
                        // XXX how about content_source_url
                        articles.add(article);
                        logger.info("Pre-cached article: {}", article.getTitle());
                        break;
                    }
                }
            }
        }
    }

    private static void loadTopicFilters() {
        File filterFile = new File(filePath);
        try {
            if (filterFile.exists()) {
                Files.lines(filterFile.toPath(), StandardCharsets.UTF_8).forEach(line -> {
                    if (StringUtils.isNotBlank(line)) {
                        TopicFilter filter = Json.decodeValue(line, TopicFilter.class);
                        topicFilters.add(filter);
                        logger.info("Topic Filter: {}", filter.getTopic());
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReplyXMLFormat handle(IncomingMassSendJobFinishEventMessage incoming) {
        // Update news material filter result when mass send job finished
        materialApi.batchGet("news", 0, 1, ClassifyNewsHandler::filterTopicArticles);
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
