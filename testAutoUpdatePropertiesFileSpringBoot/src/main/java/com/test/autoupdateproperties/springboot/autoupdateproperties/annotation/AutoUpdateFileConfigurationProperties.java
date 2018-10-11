package com.test.autoupdateproperties.springboot.autoupdateproperties.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoUpdateFileConfigurationProperties {
    String value() default "";

    String path() default "";
}
