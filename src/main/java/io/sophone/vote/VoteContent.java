package io.sophone.vote;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eyakcn on 2014/10/13.
 */
public class VoteContent {
    public String id;
    public String title = "(Empty Title)";
    public String image;
    public String text;
    public int count;
    public int minSelection = 1;
    public int maxSelection = 1;
    public List<VoteCandidate> candidates = new ArrayList<>();
}
