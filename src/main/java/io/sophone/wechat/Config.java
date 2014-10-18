package io.sophone.wechat;

import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
public class Config {
    public static final Identity wechatId;
    private static final String baseDir = System.getProperty("user.home") + "/wechat/";

    static {
        wechatId = loadIdentity();
    }

    private static Identity loadIdentity() {
        // Init Wechat app identity
        File idFile = new File(baseDir + "identity.json");
        if (!idFile.exists()) {
            throw new RuntimeException("No identity.json found. baseDir=" + baseDir);
        }
        String idContent = null;
        try {
            idContent = new String(Files.readAllBytes(idFile.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Json.decodeValue(idContent, Identity.class);
    }
}
