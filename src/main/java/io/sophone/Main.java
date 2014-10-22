package io.sophone;

import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.middleware.BodyParser;
import com.jetdrone.vertx.yoke.middleware.ErrorHandler;
import com.jetdrone.vertx.yoke.middleware.Limit;
import com.jetdrone.vertx.yoke.middleware.Static;
import io.sophone.engine.ThymeleafEngine;
import io.sophone.questionnaire.QuestionnaireStatisticHandler;
import io.sophone.vote.WechatVoteHandler;
import org.vertx.java.platform.Verticle;

/**
 * Created by eyakcn on 2014/8/20.
 */
public class Main extends Verticle {

    @Override
    public void start() {
        WechatVoteHandler.setHttpClient(vertx.createHttpClient());
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
        app.use("/webroot/", new Static("webroot", 0));
        app.use("/sysroot/", new Static(System.getProperty("user.home") + "/wechat/", 0));
        app.listen(8181);
    }
}
