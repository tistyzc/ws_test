package com.test.autoupdateproperties.springboot.autoupdateproperties.annotation;

import java.lang.annotation.*;

/**
 * 标记能自动更新的配置文件注解，说明：path必须指定，建议指定为classpath路径
 *
 * @author yuzc
 * @date 20181012
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoUpdateFileConfigurationProperties {

    /**
     * 配置文件地址，建议指定为classpath路径
     */
    String path() default "";
}
