package io.sophone;

import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.middleware.BodyParser;
import com.jetdrone.vertx.yoke.middleware.ErrorHandler;
import com.jetdrone.vertx.yoke.middleware.Limit;
import com.jetdrone.vertx.yoke.middleware.Static;
import org.vertx.java.platform.Verticle;

/**
 * Created by eyakcn on 2014/8/20.
 */
public class Main extends Verticle {

    @Override
    public void start() {
        Yoke app = new Yoke(vertx);
        app.use(new ErrorHandler(true));
        app.use(new Limit(4096));
        app.use(new BodyParser());
        app.use(new Static("webroot", 0, true, false));
        app.listen(8181);
    }
}
