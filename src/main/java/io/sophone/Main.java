package io.sophone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.engine.GroovyTemplateEngine;
import com.jetdrone.vertx.yoke.engine.StringPlaceholderEngine;
import com.jetdrone.vertx.yoke.middleware.*;
import io.sophone.engine.ThymeleafEngine;
import io.sophone.questionnaire.QuestionnaireStatisticHandler;
import io.sophone.vote.WechatVoteHandler;
import io.sophone.wechat.Identity;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.platform.Verticle;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by eyakcn on 2014/8/20.
 */
public class Main extends Verticle {
    private static final Identity wechatId;
    private static final String baseDir = System.getProperty("user.home") + "/";

    static {
        // Init Wechat app identity
        File idFile = new File(baseDir + "identity.json");
        if (!idFile.exists()) {
            throw new RuntimeException("No identity.json found. baseDir=" + baseDir);
        }
        try {
            wechatId = new Gson().fromJson(new String(Files.readAllBytes(idFile.toPath())), Identity.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        WechatVoteHandler.setHttpClient(vertx.createHttpClient());
        WechatVoteHandler.setIdentity(wechatId);
        WechatVoteHandler.setContainer(container);

        QuestionnaireStatisticHandler.setContainer(container);

        Yoke app = new Yoke(vertx);
        app.set("title", "COMEARLY");
        app.engine(new ThymeleafEngine("webroot"));
        app.use(new ErrorHandler(true));
        app.use(new Limit(4096));
        app.use(new BodyParser());
        app.use("/wechat/vote", new WechatVoteHandler());
        app.use("/osaka/counting", new QuestionnaireStatisticHandler());
        app.use("/webroot", new Static("webroot", 0, true, false));
        app.listen(8181);
    }
}
