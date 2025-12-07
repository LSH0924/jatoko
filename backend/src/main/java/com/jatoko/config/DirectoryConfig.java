package com.jatoko.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.dir")
public class DirectoryConfig {
    private String target;
    private String translated;
}
