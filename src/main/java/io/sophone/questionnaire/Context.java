package io.sophone.questionnaire;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
class Context {
    static final String delimiter = " | ";
    static final String baseDir = System.getProperty("user.home") + "/";
    static final String answerFilePath = baseDir + "questionnaire.txt";

    static final List<Map<String, Integer>> statistic = new ArrayList<>();
    static final List<List<String>> parsedAnswers = new ArrayList<>();

    static {
        File baseDirFile = new File(baseDir);
        baseDirFile.mkdirs();

        loadAnswerFile();
    }

    private static void loadAnswerFile() {
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


    static void analyzeLine(String line) {
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
}
