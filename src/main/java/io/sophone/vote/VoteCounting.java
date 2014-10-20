package io.sophone.vote;

import io.sophone.wechat.SnsUser;

import java.util.*;

/**
 * Created by eyakcn on 2014/10/15.
 */
public class VoteCounting {
    private final String title;
    private final Map<String, List<String>> userSelectionsMap = new HashMap<>();
    private final Map<String, Map<String, SnsUser>> selectionUsersMap = new HashMap<>();
    private final Set<SnsUser> userSet = new HashSet<>();

    public VoteCounting(String title) {
        this.title = title;
    }

    public List<String> fetchUserSelections(String openid) {
        return userSelectionsMap.get(openid);
    }

    public void recordUserSelections(String openid, List<String> selections) {
        userSelectionsMap.put(openid, selections);
    }

    public void removeUserFromSelections(List<String> selections, String openid) {
        for (String selection : selections) {
            Map<String, SnsUser> usersMap = selectionUsersMap.get(selection);
            if (usersMap != null) {
                usersMap.remove(openid);
            }
        }
    }

    public void addUserToSelections(List<String> selections, SnsUser user) {
        for (String selection : selections) {
            Map<String, SnsUser> usersMap = selectionUsersMap.get(selection);
            if (usersMap == null) {
                usersMap = new HashMap<>();
                selectionUsersMap.put(selection, usersMap);
            }
            usersMap.put(user.openid, user);
        }
    }

    public void recordUser(SnsUser user) {
        if (Objects.nonNull(user)) {
            userSet.add(user);
        }
    }

    public int usersCount() {
        return userSet.size();
    }

    public int usersCountOf(String caption) {
        Map<String, SnsUser> userMap = selectionUsersMap.get(caption);
        return Objects.isNull(userMap) ? 0 : userMap.size();
    }
}
