package io.sophone.vote;

import io.sophone.wechat.SnsUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by eyakcn on 2014/10/15.
 */
public class VoteCounting {
    private final String title;

    public VoteCounting(String title) {
        this.title = title;
    }

    private final Map<String, List<String>> userSelectionsMap = new HashMap<>();

    public List<String> fetchUserSelections(String openid) {
        return userSelectionsMap.get(openid);
    }

    public void recordUserSelections(String openid, List<String> selections) {
        userSelectionsMap.put(openid, selections);
    }

    private final Map<String, Map<String, SnsUser>> selectionUsersMap = new HashMap<>();

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
}
