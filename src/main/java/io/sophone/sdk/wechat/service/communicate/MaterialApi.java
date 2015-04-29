package io.sophone.sdk.wechat.service.communicate;

import com.google.gson.Gson;
import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.MaterialItem;
import io.sophone.sdk.wechat.model.MaterialList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.json.impl.Json;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public class MaterialApi {
    private static final Logger logger = LoggerFactory.getLogger(MaterialApi.class);
    private static final String BATCHGET_URL = "/cgi-bin/material/batchget_material?access_token={0}";
    private static final int MAX_PER_REQ = 20;

    private final WechatConfig config;
    private final HttpClient http;
    private final BasicApi basicApi;

    public MaterialApi(WechatConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
        this.basicApi = new BasicApi(config, http);
    }

    private void innerBatchGet(String type, int offset, int count, Consumer<MaterialList> materialConsumer) {
        Map<String, Object> postModel = new HashMap<>();
        postModel.put("type", type);
        postModel.put("offset", offset);
        postModel.put("count", count);
        final String postJsonStr = Json.encode(postModel);

        basicApi.getAccessToken(token -> {
            final String batchGetUrl = MessageFormat.format(BATCHGET_URL, token.access_token);
            logger.info("Fetch {} materials [{} + {}]: {}\n{}", type, offset, count, batchGetUrl, postJsonStr);
            HttpClientRequest batchGetReq = http.post(batchGetUrl, userRes -> userRes.bodyHandler(userResBody -> {
                final String line = userResBody.toString();
                final MaterialList materialList = new Gson().fromJson(line, MaterialList.class);
                if (materialList.isSuccess()) {
                    logger.info("{} + {}: total({}), items({})", offset, count, materialList.total_count, materialList.item_count);
                    materialConsumer.accept(materialList);
                } else {
                    logger.error(materialList.errcode + ": " + materialList.errmsg);
                }
            }));
            batchGetReq.putHeader("Content-Type", "application/json");
            batchGetReq.putHeader("Content-Length", postJsonStr.getBytes(StandardCharsets.UTF_8).length + "");
            batchGetReq.write(postJsonStr);
            batchGetReq.exceptionHandler(t -> logger.error("Request to fetch materials failed.", t));
            batchGetReq.end();
        });
    }

    public void batchGet(String type, int offset, int count, Consumer<List<MaterialItem>> materialConsumer) {
        innerBatchGet(type, offset, count, materialList -> {
            materialConsumer.accept(materialList.item);
        });
    }

    public void batchGetAll(String type, Consumer<List<MaterialItem>> materialConsumer) {
        final int count = MAX_PER_REQ;
        innerBatchGet(type, 0, count, materialList -> {
            materialConsumer.accept(materialList.item);

            final int mod = materialList.total_count / count;
            for (int offset = count; offset <= count * mod; offset += count) {
                batchGet(type, offset, count, materialConsumer);
            }
        });
    }
}
