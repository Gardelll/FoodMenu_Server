package cn.sunxinao.menu.server.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {

    public static final String APP_HOME = System.getProperty("user.home", System.getenv("HOME")) + "/hist-menu";

    public static final String UPLOAD_DIR = APP_HOME + "/uploads";

    public static final Properties DB_PROPERTIES;

    static {
        DB_PROPERTIES = new Properties();
        try (InputStream inputStream = new FileInputStream(APP_HOME + "/database_config.properties")) {
            DB_PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
