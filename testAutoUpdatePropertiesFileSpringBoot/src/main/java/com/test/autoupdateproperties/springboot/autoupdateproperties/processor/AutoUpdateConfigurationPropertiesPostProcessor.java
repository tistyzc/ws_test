package com.test.autoupdateproperties.springboot.autoupdateproperties.processor;

import com.geekplus.optimus.common.util.io.FileChangeMonitor;
import com.test.autoupdateproperties.springboot.autoupdateproperties.annotation.AutoUpdateFileConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.*;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

@Slf4j
public class AutoUpdateConfigurationPropertiesPostProcessor implements BeanPostProcessor, ApplicationContextAware, BeanFactoryAware, InitializingBean {
    private ApplicationContext applicationContext;

    private BeanFactory beanFactory;

    private ConversionService conversionService;

    private DefaultConversionService defaultConversionService;
    private List<Converter<?, ?>> converters = Collections.emptyList();

    private List<GenericConverter> genericConverters = Collections.emptyList();

    private static final String CLASSPATH_START = "/\\ \t";

    @Autowired(required = false)
    @ConfigurationPropertiesBinding
    public void setConverters(List<Converter<?, ?>> converters) {
        this.converters = converters;
    }

    @Autowired(required = false)
    @ConfigurationPropertiesBinding
    public void setGenericConverters(List<GenericConverter> converters) {
        this.genericConverters = converters;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        AutoUpdateFileConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), AutoUpdateFileConfigurationProperties.class);

        if (annotation != null) {
            postProcessBeforeInitialization(bean, beanName, annotation);
        }

        return bean;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.conversionService == null) {
            this.conversionService = getOptionalBean(
                    ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
                    ConversionService.class);
        }
    }

    private <T> T getOptionalBean(String name, Class<T> type) {
        try {
            return this.beanFactory.getBean(name, type);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void postProcessBeforeInitialization(Object bean, String beanName, AutoUpdateFileConfigurationProperties annotation) {
        log.info("postProcessBeforeInitialization", "bean", beanName);

        String path = annotation.path();

        if (!path.isEmpty()) {
            if (path.startsWith("classpath:")) {
                path = path.substring(10);

                while (CLASSPATH_START.indexOf(path.charAt(0)) >= 0) {
                    path = path.substring(1);
                }

                path = Thread.currentThread().getContextClassLoader().getResource(path).getPath();
            }

            final String finalPath = path;

            FileChangeMonitor.getInstance().addListener(path, (p) -> {
                log.info("postProcessBeforeInitialization file changed", "path", finalPath, "bean", beanName);
                fileChanged(bean, beanName, finalPath);
            });
        }
    }

    private void fileChanged(Object bean, String beanName, String path) {
        ConfigurationProperties annotation = AnnotationUtils
                .findAnnotation(bean.getClass(), ConfigurationProperties.class);
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(bean);
        factory.setPropertySources(getPropertySources(beanName, path));
        factory.setValidator(null);
        // If no explicit conversion service is provided we add one so that (at least)
        // comma-separated arrays of convertibles can be bound automatically
        factory.setConversionService(this.conversionService == null
                ? getDefaultConversionService() : this.conversionService);
        if (annotation != null) {
            factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
            factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
            factory.setExceptionIfInvalid(annotation.exceptionIfInvalid());
            factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
            if (StringUtils.hasLength(annotation.prefix())) {
                factory.setTargetName(annotation.prefix());
            }
        }
        try {
            factory.bindPropertiesToTarget();
        } catch (Exception ex) {
            log.warn("fileChanged bindPropertiesToTarget exception", ex);
        }
    }

    private PropertySources getPropertySources(String beanName, String path)
    {
        Properties props = new Properties();

        try (InputStream in = new BufferedInputStream(new FileInputStream(
                new File(path)))) {
            props.load(in);
        } catch (Exception e) {
            log.warn("getPropertySources exception", "path", path, e);
        }

        return new FilePropertySources(new PropertiesPropertySource("filePropertySource-" + beanName, props));
    }

    private ConversionService getDefaultConversionService() {
        if (this.defaultConversionService == null) {
            DefaultConversionService conversionService = new DefaultConversionService();
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
            for (Converter<?, ?> converter : this.converters) {
                conversionService.addConverter(converter);
            }
            for (GenericConverter genericConverter : this.genericConverters) {
                conversionService.addConverter(genericConverter);
            }
            this.defaultConversionService = conversionService;
        }
        return this.defaultConversionService;
    }

    private static class FilePropertySources implements PropertySources {

        private PropertySource propertySource;

        FilePropertySources(PropertySource propertySource) {
            this.propertySource = propertySource;
        }

        @Override
        public Iterator<PropertySource<?>> iterator() {
            MutablePropertySources result = getFlattened();
            return result.iterator();
        }

        @Override
        public boolean contains(String name) {
            return get(name) != null;
        }

        @Override
        public PropertySource<?> get(String name) {
            return getFlattened().get(name);
        }

        private MutablePropertySources getFlattened() {
            MutablePropertySources result = new MutablePropertySources();
            flattenPropertySources(propertySource, result);
            return result;
        }

        private void flattenPropertySources(PropertySource<?> propertySource,
                                            MutablePropertySources result) {
            Object source = propertySource.getSource();
            if (source instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
                for (PropertySource<?> childSource : environment.getPropertySources()) {
                    flattenPropertySources(childSource, result);
                }
            } else {
                result.addLast(propertySource);
            }
        }

    }

    private static class FilePropertySource extends PropertiesPropertySource {
        public FilePropertySource(String name, Properties source) {
            super(name, source);
        }
    }
}
