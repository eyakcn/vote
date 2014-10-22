package io.sophone.questionnaire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.platform.Container;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by eyakcn on 2014/10/14.
 */
public class QuestionnaireStatisticHandler extends Middleware {

    private static Container container;

    public static void setContainer(Container container_) {
        container = container_;
    }

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        switch (request.method()) {
            case "GET":
                handleGet(request, next);
                break;
            case "PUT":
                break;
            case "POST":
                handlePost(request, next);
                break;
            case "DELETE":
                break;
            case "OPTIONS":
                break;
            case "HEAD":
                break;
            case "TRACE":
                break;
            case "PATCH":
                break;
            case "CONNECT":
                break;
        }
    }

    private void handlePost(YokeRequest request, Handler<Object> next) {
        if (request.body() == null) {
            next.handle(null);
            return;
        }
        List<String> answers = ((JsonArray) request.body()).toList();
        String line = StringUtils.join(answers, Context.delimiter);
        Context.analyzeLine(line);

        List<String> lines = new ArrayList<String>();
        lines.add(line);
        File answerFile = new File(Context.answerFilePath);
        try {
            Files.write(answerFile.toPath(), lines, StandardOpenOption.APPEND);
            container.logger().info("New answer add to: " + Context.answerFilePath);
        } catch (IOException e) {
            container.logger().error("Failed to write answers file!" + e.toString());
        }

//        vertx.fileSystem().writeFile(answerFilePath, new Buffer(line).appendString("\n"), result -> {
//            if (!result.succeeded()) {
//                container.logger().error("Failed to write answers file!" + result.cause());
//            } else {
//                container.logger().info("New answer add to: " + answerFilePath);
//            }
//        });

        String ipAddress = request.ip();
        container.logger().info("Submit from: " + ipAddress);
        request.response().end();
    }

    private void handleGet(YokeRequest request, Handler<Object> next) {
        GetResult returnVal = new GetResult();
        returnVal.statistic = Context.statistic;
        returnVal.answers = Context.parsedAnswers;
        Gson gson = new GsonBuilder().create();
        String content = gson.toJson(returnVal);
        container.logger().info("Get Result: " + content);

        request.response().putHeader("Content-Type", "application/json");
        try {
            request.response().putHeader("Content-Length", "" + content.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException e) {
            next.handle(e);
        }
        request.response().write(content);
        request.response().end();
    }

    private static class GetResult {
        public List<Map<String, Integer>> statistic;
        public List<List<String>> answers;
    }
}
