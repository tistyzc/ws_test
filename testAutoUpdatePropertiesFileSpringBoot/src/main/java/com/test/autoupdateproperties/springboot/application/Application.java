package com.test.autoupdateproperties.springboot.application;

import com.test.autoupdateproperties.springboot.autoupdateproperties.annotation.EnableAutoUpdateFileConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 基本跑通
 *
 * @author yuzc
 * @date 20181012
 */

@EnableAutoConfiguration
@EnableAutoUpdateFileConfigurationProperties
@Configuration
@EnableWebMvc
@ComponentScan("com.test.*")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Slf4j
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
