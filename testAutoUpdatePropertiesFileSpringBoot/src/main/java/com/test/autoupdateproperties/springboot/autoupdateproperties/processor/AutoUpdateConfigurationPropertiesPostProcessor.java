package com.test.autoupdateproperties.springboot.autoupdateproperties.processor;

import com.geekplus.optimus.common.util.io.FileChangeMonitor;
import com.test.autoupdateproperties.springboot.autoupdateproperties.annotation.AutoUpdateFileConfigurationProperties;
import lombok.Data;
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
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 处理自动更新配置的PostProcessor
 * <p>
 * 实现思路：
 * 1、底层使用PropertiesConfigurationFactory来实现属性绑定；
 * 2、属性来源使用自定义的PropertySource，内容是自己从文件里面读出来的；
 * 3、属性替换的实现是先把属性读到一个新对象上，然后再覆盖到原来的配置对象；
 * 4、配置文件监视依赖于optimus-common-util中的FileChangeMonitor
 * <p>
 * 目前存在的问题：
 * 1、属性是一个个从新对象到原有对象的，在并发时可能存在一个属性更新了，另一个未更新的情况
 *
 * @author yuzc
 * @date 20181012
 */
@Slf4j
public class AutoUpdateConfigurationPropertiesPostProcessor implements BeanPostProcessor, ApplicationContextAware, BeanFactoryAware, InitializingBean {
    private ApplicationContext applicationContext;

    private BeanFactory beanFactory;

    private ConversionService conversionService;

    private DefaultConversionService defaultConversionService;

    private List<Converter<?, ?>> converters = Collections.emptyList();

    private List<GenericConverter> genericConverters = Collections.emptyList();

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
            File file = null;

            try {
                file = ResourceUtils.getFile(path);
            } catch (FileNotFoundException e) {
                log.warn("getFile exception", "path", path);
            }

            if (file != null) {
                final String finalPath = file.getAbsolutePath();
                PropertyBindContext context = loadOriginalProperties(bean);

                FileChangeMonitor.getInstance().addListener(finalPath, p -> {
                    log.info("postProcessBeforeInitialization file changed", "path", finalPath, "bean", beanName);
                    fileChanged(bean, beanName, finalPath, context);
                });
            }
        }
    }

    private PropertyBindContext loadOriginalProperties(Object bean) {
        PropertyBindContext context = new PropertyBindContext();

        loadBeanFields(bean, context);
        Map<String, Object> originalProperties = new HashMap<>(context.getFieds().size());

        for (PropertyBindField field : context.getFieds()) {
            Object obj = field.get(bean);

            if (obj != null) {
                originalProperties.put(field.getName(), obj);
            }
        }

        context.setOriginalProperties(originalProperties);
        return context;
    }

    private void loadBeanFields(Object bean, PropertyBindContext context) {
        Method[] methods = bean.getClass().getMethods();
        Map<String, Method> mapMethods = new HashMap<>(methods.length);
        List<PropertyBindField> fields = new ArrayList<>();

        for (Method method : methods) {
            mapMethods.put(method.getName(), method);
        }

        for (Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                Method getter = mapMethods.get("get" + method.getName().substring(3));

                if (getter != null && getter.getParameterCount() == 0) {
                    PropertyBindField field = new PropertyBindField();
                    field.setGetter(getter);
                    field.setSetter(method);
                    field.setName(method.getName().substring(3));
                    fields.add(field);
                }
            }
        }

        context.setFieds(fields);
    }

    private void propertyBind(Object bean, String beanName, String path) {
        ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<>(bean);
        factory.setPropertySources(getPropertySources(beanName, path));
        factory.setValidator(null);

        factory.setConversionService(this.conversionService == null
                ? getDefaultConversionService() : this.conversionService);

        if (annotation != null) {
            factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
            factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
            factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
            if (StringUtils.hasLength(annotation.prefix())) {
                factory.setTargetName(annotation.prefix());
            }
        }

        try {
            factory.bindPropertiesToTarget();
        } catch (Exception ex) {
            log.warn("propertyBind bindPropertiesToTarget exception", ex);
        }
    }

    private void fileChanged(Object bean, String beanName, String path, PropertyBindContext context) {
        Object newBean = null;

        try {
            newBean = bean.getClass().newInstance();
        } catch (Exception e) {
            log.warn("fileChanged newInstance exception", "class", bean.getClass().getSimpleName(), e);
        }

        if (newBean == null) {
            return;
        }

        propertyBind(newBean, beanName, path);
        propertyMerge(bean, newBean, context);
    }

    private void propertyMerge(Object bean, Object newBean, PropertyBindContext context) {
        for (PropertyBindField field : context.getFieds()) {
            Object newValue = field.get(newBean);

            if (newValue == null) {
                newValue = context.getOriginalProperties().get(field.getName());
            }

            if (newValue != null) {
                field.set(bean, newValue);
            }
        }
    }

    private PropertySources getPropertySources(String beanName, String path) {
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
            DefaultConversionService service = new DefaultConversionService();
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

            for (Converter<?, ?> converter : this.converters) {
                service.addConverter(converter);
            }

            for (GenericConverter genericConverter : this.genericConverters) {
                service.addConverter(genericConverter);
            }

            this.defaultConversionService = service;
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

    @Data
    private static class PropertyBindContext {
        Map<String, Object> originalProperties;
        List<PropertyBindField> fieds;
    }

    @Data
    private static class PropertyBindField {
        private String name;
        private Method setter;
        private Method getter;

        public Object get(Object object) {
            if (getter == null) {
                return null;
            }

            try {
                return getter.invoke(object);
            } catch (Exception e) {
                log.warn("get invoke exception", "name", name, e);
                return null;
            }
        }

        public void set(Object object, Object value) {
            if (setter != null) {
                try {
                    setter.invoke(object, value);
                } catch (Exception e) {
                    log.warn("set invoke exception", "name", name, e);
                }
            }
        }
    }
}
