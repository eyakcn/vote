package io.sophone;

import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.middleware.BodyParser;
import com.jetdrone.vertx.yoke.middleware.ErrorHandler;
import com.jetdrone.vertx.yoke.middleware.Limit;
import io.sophone.override.Static;
import io.sophone.override.ThymeleafEngine;
import io.sophone.questionnaire.QuestionnaireStatisticHandler;
import io.sophone.vote.WechatVoteBulletinHandler;
import io.sophone.vote.WechatVoteHandler;
import io.sophone.weixin.WeixinHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

/**
 * Created by eyakcn on 2014/8/20.
 */
public class Main extends Verticle {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start() {
        logger.info("Temp dir: " + System.getProperty("java.io.tmpdir"));

        QuestionnaireStatisticHandler.setContainer(container);

        Yoke app = new Yoke(vertx);
        app.set("title", "COMEARLY");
        app.engine(new ThymeleafEngine("webroot"));
        app.use(new ErrorHandler(true));
        app.use(new Limit(4096));
        app.use(new BodyParser());
        app.use("/weixin", new WeixinHandler(this));
        app.use("/wechat/vote", new WechatVoteHandler(this));
        app.use("/wechat/bulletin/vote", new WechatVoteBulletinHandler());
        app.use("/osaka/counting", new QuestionnaireStatisticHandler());
        app.use("/webroot/", new Static("webroot/", 0));
        app.use("/sysroot/", new Static(System.getProperty("user.home") + "/", 0));
        app.listen(80); // XXX Weixin server requires to use port 80
    }
}
