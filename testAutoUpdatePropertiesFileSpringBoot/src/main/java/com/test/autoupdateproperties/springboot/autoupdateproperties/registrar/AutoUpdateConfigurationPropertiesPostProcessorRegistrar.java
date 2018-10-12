package com.test.autoupdateproperties.springboot.autoupdateproperties.registrar;

import com.test.autoupdateproperties.springboot.autoupdateproperties.processor.AutoUpdateConfigurationPropertiesPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

/**
 * PostProcessor注册
 *
 * @author yuzc
 * @date 20181012
 */
@Component
public class AutoUpdateConfigurationPropertiesPostProcessorRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String BINDER_BEAN_NAME = AutoUpdateConfigurationPropertiesPostProcessor.class.getName();

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(BINDER_BEAN_NAME)) {
            BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(
                    AutoUpdateConfigurationPropertiesPostProcessor.class);
            registry.registerBeanDefinition(BINDER_BEAN_NAME, bean.getBeanDefinition());
        }
    }
}
