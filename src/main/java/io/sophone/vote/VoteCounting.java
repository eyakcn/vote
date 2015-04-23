package io.sophone.vote;

import io.sophone.sdk.wechat.model.User;
import org.vertx.java.core.impl.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by eyakcn on 2014/10/15.
 */
public class VoteCounting {
    private final String id;
    private final Map<String, List<String>> voterChoicesMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, User>> choiceVotersMap = new ConcurrentHashMap<>();
    private final Set<User> voterSet = new ConcurrentHashSet<>();

    public VoteCounting(String id) {
        this.id = id;
    }

    public List<String> fetchVoterChoices(String openid) {
        return voterChoicesMap.get(openid);
    }

    public void recordVoterChoices(String openid, List<String> choices) {
        voterChoicesMap.put(openid, choices);
    }

    public void removeVoterFromChoices(List<String> choices, String openid) {
        for (String choice : choices) {
            Map<String, User> usersMap = choiceVotersMap.get(choice);
            if (usersMap != null) {
                usersMap.remove(openid);
            }
        }
    }

    public void addVoterToChoices(List<String> choices, User voter) {
        for (String chioce : choices) {
            Map<String, User> usersMap = choiceVotersMap.get(chioce);
            if (usersMap == null) {
                usersMap = new ConcurrentHashMap<>();
                choiceVotersMap.put(chioce, usersMap);
            }
            usersMap.put(voter.openid, voter);
        }
    }

    public void recordVoter(User voter) {
        if (Objects.nonNull(voter)) {
            voterSet.add(voter);
        }
    }

    public int getVotersCount() {
        return voterSet.size();
    }

    public List<User> getVotersOf(String caption) {
        Map<String, User> voterMap = choiceVotersMap.get(caption);
        return Objects.isNull(voterMap) ? new ArrayList<>() : new ArrayList<>(voterMap.values());
    }

    public int getVotersCountOf(String caption) {
        Map<String, User> voterMap = choiceVotersMap.get(caption);
        return Objects.isNull(voterMap) ? 0 : voterMap.size();
    }

    public boolean alreadyVoted(String openid) {
        return voterChoicesMap.containsKey(openid);
    }
}
