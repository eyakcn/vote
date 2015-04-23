package io.sophone.vote;

import com.google.gson.Gson;
import com.jetdrone.vertx.yoke.core.JSON;
import io.sophone.sdk.wechat.model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
class Context {
    static final Map<String, User> userMap = new ConcurrentHashMap<>();
    static final Map<String, VoteCounting> voteCountingMap = new ConcurrentHashMap<>();
    static final Map<String, VoteContent> voteContentMap = new ConcurrentHashMap<>();
    static final String baseDir = System.getProperty("user.home") + "/wechat/vote/";
    static final String userFilePath = baseDir + "vote_users.txt";
    static final String answerFilePath = baseDir + "vote_history.txt";
    static final String contentFilePath = baseDir + "vote_content.txt";
    private static final Logger logger = LoggerFactory.getLogger(Context.class);

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
                // FIXME it's a bad idea to write each vote content in single line
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
                    User user = Json.decodeValue(line, User.class);
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
        List<String> selections = (List<String>) answer.getArray("selections").toList();

        VoteCounting counting = voteCountingMap.get(contentId);
        if (Objects.isNull(counting)) {
            counting = new VoteCounting(contentId);
            voteCountingMap.put(contentId, counting);
        }

        User user = userMap.get(openid);
        if (Objects.isNull(user)) {
            throw new RuntimeException("Some one does not submit voting on detail.html.");
        }
        List<String> prevSelections = counting.fetchVoterChoices(openid);
        if (Objects.nonNull(prevSelections)) {
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

    private static void recordUserLine(String userLine) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(userLine);
        File userFile = new File(Context.userFilePath);
        Files.write(userFile.toPath(), lines, StandardOpenOption.APPEND);
    }

    static void recordUser(User user, String line) {
        Context.userMap.put(user.openid, user);
        String userLine = line;
        if (Objects.isNull(userLine)) {
            userLine = new Gson().toJson(user).replace("\r", "").replace("\n", "");
        }
        try {
            Context.recordUserLine(userLine);
            logger.info("New user add to: " + Context.userFilePath);
        } catch (IOException e) {
            logger.error("Failed to write user file!" + e.toString());
        }
    }

    static List<String> getVotedContentIds(String openid) {
        List<String> ids = voteCountingMap.entrySet().stream()
                .filter(entry -> entry.getValue().alreadyVoted(openid))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
        return ids;
    }

    static List<String> getVoteChoices(String contentId, String openid) {
        VoteCounting counting = voteCountingMap.get(contentId);
        if (Objects.isNull(counting)) {
            return Collections.EMPTY_LIST;
        }
        List<String> choices = counting.fetchVoterChoices(openid);
        return Objects.nonNull(choices) ? choices : Collections.EMPTY_LIST;
    }
}
