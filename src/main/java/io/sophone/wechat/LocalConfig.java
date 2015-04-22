package io.sophone.wechat;

import io.sophone.sdk.wechat.WechatConfig;
import io.sophone.sdk.wechat.model.Identity;
import org.vertx.java.core.json.impl.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by yuanyuan on 10/18/14 AD.
 */
public class LocalConfig implements WechatConfig {
    private static final Identity wechatId;
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

    @Override
    public String getAppId() {
        return wechatId.getAppid();
    }

    @Override
    public String getAppSecret() {
        return wechatId.getSecret();
    }

    @Override
    public String getToken() {
        return wechatId.getToken();
    }

    @Override
    public String getAESKey() {
        return wechatId.getAeskey();
    }

    @Override
    public String getOriginId() {
        return wechatId.getOriginid();
    }
}
