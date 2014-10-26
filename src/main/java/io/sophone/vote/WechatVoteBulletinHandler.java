package io.sophone.vote;

import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import io.sophone.wechat.SnsUser;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by yuanyuan on 10/26/14 AD.
 */
public class WechatVoteBulletinHandler extends Middleware {
    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        if (!Objects.equals(request.method(), "GET")) {
            next.handle(null);
            return;
        }
        String contentId = request.getParameter("content-id");
        if (Objects.isNull(contentId)) {
            List<VoteContent> contents = Context.getVoteContentList();
            request.put("contents", contents);
            request.response().render("wechat/vote/bulletin/index.html");
        } else {
            VoteContent content = Context.getVoteContent(contentId);
            request.put("content", content);

            Map<String, String> votersMap = getVotersMap(content);
            request.put("votersMap", votersMap);

            request.response().render("wechat/vote/bulletin/detail.html");
        }
    }

    public Map<String, String> getVotersMap(VoteContent content) {
        VoteCounting counting = Context.voteCountingMap.get(content.id);

        return content.candidates.stream().map(candidate -> {
            List<SnsUser> voters = counting.getVotersOf(candidate.caption);
            List<String> voterNames = voters.stream().map(voter -> {
                return voter.nickname;
            }).collect(Collectors.toList());

            String voterNamesText = StringUtils.join(voterNames, ", ");
            return new Pair<String, String>(candidate.caption, voterNamesText);

        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
}
