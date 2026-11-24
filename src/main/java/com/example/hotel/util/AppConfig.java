package com.example.hotel.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private AppConfig() { }

    public static Path databasePath() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (stream == null) {
                throw new IOException("Missing application.properties");
            }
            properties.load(stream);
        }
        return Path.of(System.getProperty("user.home"),
                properties.getProperty("database.file", ".hotel-reservation/hotel.db"));
    }
}
