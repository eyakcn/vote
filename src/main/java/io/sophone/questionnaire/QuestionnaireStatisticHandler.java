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
import java.util.*;

/**
 * Created by eyakcn on 2014/10/14.
 */
public class QuestionnaireStatisticHandler extends Middleware {
    private static final String delimiter = " | ";
    private static final String baseDir = System.getProperty("user.home") + "/";
    private static final String answerFilePath;
    private static final List<Map<String, Integer>> statistic = new ArrayList<>();
    private static final List<List<String>> parsedAnswers = new ArrayList<>();

    static {
        answerFilePath = baseDir + "questionnaire.txt";
        File answerFile = new File(answerFilePath);
        try {
            if (answerFile.exists()) {
                List<String> lines = Files.readAllLines(answerFile.toPath());
                lines.forEach(line -> analyzeLine(line));
            } else {
                answerFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Container container;

    public static void setContainer(Container container_) {
        container = container_;
    }

    private static void analyzeLine(String line) {
        if (line == null || line.length() == 0) {
            return;
        }
        String[] answers = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, delimiter);
        for (int i = 0; i < answers.length; i++) {
            String answer = answers[i];

            Map<String, Integer> answerMap = null;
            if (statistic.size() == i) {
                answerMap = new LinkedHashMap<>(6); // at most 6 choices
                statistic.add(answerMap);
            } else {
                answerMap = statistic.get(i);
            }

            if (answerMap.containsKey(answer)) {
                Integer count = answerMap.get(answer);
                answerMap.put(answer, ++count);
            } else {
                answerMap.put(answer, 1);
            }
        }
        parsedAnswers.add(Arrays.asList(answers));
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
            return;
        }
        List<String> answers = ((JsonArray) request.body()).toList();
        String line = StringUtils.join(answers, delimiter);
        analyzeLine(line);

        List<String> lines = new ArrayList<String>();
        lines.add(line);
        File answerFile = new File(answerFilePath);
        try {
            Files.write(answerFile.toPath(), lines, StandardOpenOption.APPEND);
            container.logger().info("New answer add to: " + answerFilePath);
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
        returnVal.statistic = statistic;
        returnVal.answers = parsedAnswers;
        Gson gson = new GsonBuilder().create();
        String content = gson.toJson(returnVal);
        container.logger().info("Get Result: " + content);

        request.response().putHeader("Content-Type", "application/json");
        try {
            request.response().putHeader("Content-Length", "" + content.getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        request.response().write(content);
        request.response().end();
    }

    private static class GetResult {
        public List<Map<String, Integer>> statistic;
        public List<List<String>> answers;
    }
}
