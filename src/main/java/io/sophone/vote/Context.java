package io.sophone.vote;

import com.jetdrone.vertx.yoke.core.JSON;
import io.sophone.wechat.SnsUser;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
class Context {
    static final Map<String, SnsUser> userMap = new HashMap<>();
    static final Map<String, VoteCounting> voteCountingMap = new HashMap<>();
    static final Map<String, VoteContent> voteContentMap = new HashMap<>();
    static final String baseDir = System.getProperty("user.home") + "/wechat/vote/";
    static final String userFilePath = baseDir + "vote_users.txt";
    static final String answerFilePath = baseDir + "vote_history.txt";
    static final String contentFilePath = baseDir + "vote_content.txt";

    static {
        File baseDirFile = new File(baseDir);
        baseDirFile.mkdirs();

        loadUserFile();
        loadAnswerFile();
        loadContentFile();
    }

    private static void loadContentFile() {
        File contentFile = new File(contentFilePath);
        try {
            if (contentFile.exists()) {
                List<String> lines = Files.readAllLines(contentFile.toPath());
                lines.forEach(line -> {
                    analyzeVoteContent(line); // need userMap prepared
                });
            } else {
                contentFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadAnswerFile() {
        File answerFile = new File(answerFilePath);
        try {
            if (answerFile.exists()) {
                List<String> lines = Files.readAllLines(answerFile.toPath());
                lines.forEach(line -> {
                    JsonObject answer = new JsonObject(JSON.<Map>decode(line));
                    analyzeAnswer(answer); // need userMap prepared
                });
            } else {
                answerFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadUserFile() {
        File userFile = new File(userFilePath);
        try {
            if (userFile.exists()) {
                List<String> lines = Files.readAllLines(userFile.toPath());
                lines.forEach(line -> {
                    SnsUser user = Json.decodeValue(line, SnsUser.class);
                    userMap.put(user.openid, user);
                });
            } else {
                userFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void analyzeAnswer(JsonObject answer) {
        String openid = answer.getString("openid");
        if (StringUtils.isBlank(openid)) {
            return;
        }
        String contentId = answer.getString("content-id");
        String time = answer.getString("time");
        List<String> selections = (List<String>) answer.getArray("selections").toList();

        VoteCounting counting = voteCountingMap.get(contentId);
        if (counting == null) {
            counting = new VoteCounting(contentId);
            voteCountingMap.put(contentId, counting);
        }

        SnsUser user = userMap.get(openid);
        if (user == null) {
            user = new SnsUser();
            user.openid = openid;
            user.nickname = openid;
            userMap.put(openid, user);
        }
        user.reserveField = time; // backup vote time into reserve field, this design seems smell
        List<String> prevSelections = counting.fetchUserSelections(openid);
        if (prevSelections != null) {
            counting.removeUserFromSelections(prevSelections, openid);
        }

        counting.addUserToSelections(selections, user);
        counting.recordUserSelections(openid, selections);
        counting.recordUser(user);
    }

    private static void analyzeVoteContent(String line) {
        VoteContent content = Json.decodeValue(line, VoteContent.class);
        voteContentMap.put(content.id, content);
    }

    public static List<String> getVotedContentIds(String openid) {
        List<String> ids = new ArrayList<>();

        return voteCountingMap.entrySet().stream().filter(entry -> {
            return entry.getValue().alreadyVoted(openid);
        }).map(entry -> {
            return entry.getKey();
        }).collect(Collectors.toList());
    }
}
