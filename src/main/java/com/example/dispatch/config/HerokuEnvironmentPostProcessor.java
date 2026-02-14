package com.example.dispatch.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Heroku の DATABASE_URL (postgres://user:pass@host:port/db) を
 * Spring Boot の spring.datasource.* プロパティに変換する。
 */
public class HerokuEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || !databaseUrl.startsWith("postgres://")) {
            return;
        }
        try {
            URI uri = new URI(databaseUrl);
            String[] userInfo = uri.getUserInfo().split(":");
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath()
                    + "?sslmode=require";

            Map<String, Object> props = new HashMap<>();
            props.put("SPRING_DATASOURCE_URL", jdbcUrl);
            props.put("SPRING_DATASOURCE_USERNAME", userInfo[0]);
            props.put("SPRING_DATASOURCE_PASSWORD", userInfo[1]);
            environment.getPropertySources().addFirst(new MapPropertySource("heroku-datasource", props));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid DATABASE_URL: " + databaseUrl, e);
        }
    }
}
