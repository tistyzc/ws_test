package com.test.autoupdateproperties.springboot.autoupdateproperties.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 标记启用自动更新配置文件的注解
 *
 * @author yuzc
 * @date 20181012
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableAutoUpdateFileConfigurationPropertiesImportSelector.class)
public @interface EnableAutoUpdateFileConfigurationProperties {
    Class<?>[] value() default {};
}
