package io.sophone.vote;

import com.jetdrone.vertx.yoke.core.JSON;
import io.sophone.wechat.SnsUser;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

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
        List<String> prevSelections = counting.fetchVoterChoices(openid);
        if (prevSelections != null) {
            counting.removeVoterFromChoices(prevSelections, openid);
        }

        counting.addVoterToChoices(selections, user);
        counting.recordVoterChoices(openid, selections);
        counting.recordVoter(user);
    }

    private static void analyzeVoteContent(String line) {
        VoteContent content = Json.decodeValue(line, VoteContent.class);
        voteContentMap.put(content.id, content);
    }

    static VoteContent getVoteContent(String contentId) {
        if (Objects.nonNull(contentId)) {
            VoteContent existContent = voteContentMap.get(contentId);
            if (Objects.nonNull(existContent)) {
                setCountingInfo(existContent);
                return existContent;
            }
        }
        Collection<VoteContent> contents = voteContentMap.values();
        VoteContent content = contents.isEmpty() ? new VoteContent() : contents.iterator().next();
        setCountingInfo(content);
        return content;
    }

    static List<VoteContent> getVoteContentList() {
        return new ArrayList<>(voteContentMap.values());
    }

    private static void setCountingInfo(VoteContent content) {
        VoteCounting voteCounting = voteCountingMap.get(content.id);
        if (Objects.isNull(voteCounting)) {
            return;
        }
        content.count = voteCounting.getVotersCount();
        for (VoteCandidate candidate : content.candidates) {
            candidate.count = voteCounting.getVotersCountOf(candidate.caption);
        }
    }
}
