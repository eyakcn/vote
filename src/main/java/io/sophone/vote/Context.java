package io.sophone.vote;

import io.sophone.wechat.SnsUser;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
class Context {
    static final Map<String, SnsUser> userMap = new HashMap<>();
    static final Map<String, VoteCounting> voteCountingMap = new HashMap<>();
    static final Map<String, VoteContent> voteContentMap = new HashMap<>();
    private static final String baseDir = System.getProperty("user.home") + "/wechat/vote/";
    static final String userFilePath = baseDir + "vote_users.txt";
    static final String answerFilePath = baseDir + "vote_history.txt";
    static final String contentFilePath = baseDir + "vote_content.txt";
    static VoteContent voteContent;

    static {
        File baseDirFile = new File(baseDir);
        baseDirFile.mkdirs();

        loadUserFile();
        loadAnswerFile();
        loadContentFile();


        voteContent = new VoteContent();
        voteContent.title = "最受欢迎实践队伍";
        voteContent.text = "经过华东政法大学第一届社会实践大赛初赛，18支优秀实践队伍脱颖而出进入决赛，同事角逐。。。";
        voteContent.count = 146;

        VoteCandidate candidate1 = new VoteCandidate();
        candidate1.caption = "A组1号";
        candidate1.text = "队伍简介：典当行业这个古老行业。。。";
        candidate1.count = 20;
        voteContent.candidates.add(candidate1);
        VoteCandidate candidate2 = new VoteCandidate();
        candidate2.caption = "A组2号";
        candidate2.text = "队伍简介：典当行业这个古老行业。。。";
        candidate2.count = 13;
        voteContent.candidates.add(candidate2);
        VoteCandidate candidate3 = new VoteCandidate();
        candidate3.caption = "A组3号";
        candidate3.text = "队伍简介：典当行业这个古老行业。。。";
        candidate3.count = 33;
        voteContent.candidates.add(candidate3);
        VoteCandidate candidate4 = new VoteCandidate();
        candidate4.caption = "A组4号";
        candidate4.text = "队伍简介：典当行业这个古老行业。。。";
        candidate4.count = 22;
        voteContent.candidates.add(candidate4);
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
                    JsonObject answer = Json.decodeValue(line, JsonObject.class);
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
        if (openid == null) {
            return;
        }
        String title = answer.getString("title");
        String time = answer.getString("time");
        List<String> selections = (List<String>) answer.getArray("selections").toList();

        VoteCounting counting = voteCountingMap.get(title);
        if (counting == null) {
            counting = new VoteCounting(title);
            voteCountingMap.put(title, counting);
        }

        SnsUser user = userMap.get(openid);
        if (user == null) {
            return;
        }
        user.reserveField = time; // backup vote time into reserve field, this design seems smell
        List<String> prevSelections = counting.fetchUserSelections(openid);
        if (prevSelections != null) {
            counting.removeUserFromSelections(prevSelections, openid);
        }

        counting.addUserToSelections(selections, user);
        counting.recordUserSelections(openid, selections);
    }

    private static void analyzeVoteContent(String line) {
        VoteContent content = Json.decodeValue(line, VoteContent.class);
        voteContentMap.put(content.title, content);
    }

}
