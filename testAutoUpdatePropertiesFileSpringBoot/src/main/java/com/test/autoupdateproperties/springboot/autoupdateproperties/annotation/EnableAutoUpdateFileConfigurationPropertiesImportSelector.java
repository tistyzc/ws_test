package com.test.autoupdateproperties.springboot.autoupdateproperties.annotation;

import com.test.autoupdateproperties.springboot.autoupdateproperties.registrar.AutoUpdateConfigurationPropertiesPostProcessorRegistrar;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 启用自动更新配置
 *
 * @author yuzc
 * @date 20181012
 */

class EnableAutoUpdateFileConfigurationPropertiesImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
                EnableAutoUpdateFileConfigurationProperties.class.getName(), false);
        Object[] type = (attributes != null) ? (Object[]) attributes.getFirst("value")
                : null;
        if (type == null || type.length == 0) {
            return new String[]{
                    AutoUpdateConfigurationPropertiesPostProcessorRegistrar.class
                            .getName()};
        }
        return new String[]{ConfigurationPropertiesBeanRegistrar.class.getName(),
                AutoUpdateConfigurationPropertiesPostProcessorRegistrar.class.getName()};
    }

    /**
     * {@link ImportBeanDefinitionRegistrar} for configuration properties support.
     */
    public static class ConfigurationPropertiesBeanRegistrar
            implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            MultiValueMap<String, Object> attributes = metadata
                    .getAllAnnotationAttributes(
                            EnableAutoUpdateFileConfigurationProperties.class.getName(), false);
            List<Class<?>> types = collectClasses(attributes.get("value"));
            for (Class<?> type : types) {
                String prefix = extractPrefix(type);
                String name = (StringUtils.hasText(prefix) ? prefix + "-" + type.getName()
                        : type.getName());
                if (!registry.containsBeanDefinition(name)) {
                    registerBeanDefinition(registry, type, name);
                }
            }
        }

        private String extractPrefix(Class<?> type) {
            ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
                    ConfigurationProperties.class);
            if (annotation != null) {
                return annotation.value();
            }
            return "";
        }

        private List<Class<?>> collectClasses(List<Object> list) {
            ArrayList<Class<?>> result = new ArrayList<Class<?>>();
            for (Object object : list) {
                for (Object value : (Object[]) object) {
                    if (value instanceof Class && value != void.class) {
                        result.add((Class<?>) value);
                    }
                }
            }
            return result;
        }

        private void registerBeanDefinition(BeanDefinitionRegistry registry,
                                            Class<?> type, String name) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(type);
            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
            registry.registerBeanDefinition(name, beanDefinition);

            AutoUpdateFileConfigurationProperties properties = AnnotationUtils.findAnnotation(type,
                    AutoUpdateFileConfigurationProperties.class);
            Assert.notNull(properties,
                    "No " + AutoUpdateFileConfigurationProperties.class.getSimpleName()
                            + " annotation found on  '" + type.getName() + "'.");
        }

    }

}
