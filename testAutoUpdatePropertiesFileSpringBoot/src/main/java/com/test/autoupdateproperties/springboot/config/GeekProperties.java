package com.test.autoupdateproperties.springboot.config;

import com.test.autoupdateproperties.springboot.autoupdateproperties.annotation.AutoUpdateFileConfigurationProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("geek")
@AutoUpdateFileConfigurationProperties(path = "classpath:application.properties")
@Component
public class GeekProperties {
    private String clientId;
    private String hermesServer;
    private Integer threadCount;
    private Integer idleMicroseconds=100;
    private Map<String, Channel> channels = new HashMap<>();


    @Data
    @NoArgsConstructor
    public static class Channel {
        private String folder;
        private String inf;
        private String url;
    }
}
