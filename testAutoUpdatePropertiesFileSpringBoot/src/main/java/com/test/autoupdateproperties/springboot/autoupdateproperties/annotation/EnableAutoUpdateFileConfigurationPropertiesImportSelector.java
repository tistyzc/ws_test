/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.test.autoupdateproperties.springboot.autoupdateproperties.annotation;

import com.test.autoupdateproperties.springboot.autoupdateproperties.registrar.AutoUpdateConfigurationPropertiesPostProcessorRegistrar;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
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
 * Import selector that sets up binding of external properties to configuration classes
 * (see {@link AutoUpdateFileConfigurationProperties}). It either registers a
 * {@link AutoUpdateFileConfigurationProperties} bean or not, depending on whether the enclosing
 * {@link EnableAutoUpdateFileConfigurationProperties} explicitly declares one. If none is declared then
 * a bean post processor will still kick in for any beans annotated as external
 * configuration. If one is declared then it a bean definition is registered with id equal
 * to the class name (thus an application context usually only contains one
 * {@link AutoUpdateFileConfigurationProperties} bean of each unique type).
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
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
            AutoUpdateFileConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
                    AutoUpdateFileConfigurationProperties.class);
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
