package com.caqy.feign;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassPathFeignClientScanner extends ClassPathBeanDefinitionScanner {
    private Class<?> markerInterface;
    private String feignBuilderName;
    private Feign.Builder feignBuilder;

    public ClassPathFeignClientScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void setMarkerInterface(Class<?> markerInterface) {
        this.markerInterface = markerInterface;
    }

    public void setFeignBuilderBeanName(String feignBuilderName) {
        this.feignBuilderName = feignBuilderName;
    }

    public void setFeignBuilder(Feign.Builder feignBuilder) {
        this.feignBuilder = feignBuilder;
    }

    public void registerFilters() {

        addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));

        // override AssignableTypeFilter to ignore matches on the actual marker interface
        if (this.markerInterface != null) {
            addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
                @Override
                protected boolean matchClassName(String className) {
                    return false;
                }
            });
        }

        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        setBeanNameGenerator((definition, registry) -> {
            if (definition instanceof AnnotatedBeanDefinition) {
                String beanName = (String) ((AnnotatedBeanDefinition) definition).getMetadata().getAnnotationAttributes(FeignClient.class.getName()).get("name");
                if (StringUtils.hasText(beanName)) {
                    return beanName;
                }
            }

            return ClassUtils.getShortName(Objects.requireNonNull(definition.getBeanClassName()));
        });

        if (!beanDefinitions.isEmpty()) {
            processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        ScannedGenericBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (ScannedGenericBeanDefinition) holder.getBeanDefinition();

            Map<String, Object> feignClientAnnotationAttr = definition.getMetadata().getAnnotationAttributes(FeignClient.class.getName());
            String url = (String) feignClientAnnotationAttr.get("url");
            Class encoderClass = (Class) feignClientAnnotationAttr.get("encoderClass");
            Class decoderClass = (Class) feignClientAnnotationAttr.get("decoderClass");

            definition.getConstructorArgumentValues().addGenericArgumentValue(definition.getBeanClassName());
            definition.setBeanClass(FeignClientFactoryBean.class);
            definition.getPropertyValues().add("url", url);

            if (StringUtils.hasText(this.feignBuilderName)) {
                definition.getPropertyValues().add("feignBuilder", new RuntimeBeanReference(this.feignBuilderName));
            } else if (this.feignBuilder != null) {
                definition.getPropertyValues().add("feignBuilder", this.feignBuilder);
            }

            try {
                if (!encoderClass.equals(Encoder.class))
                    definition.getPropertyValues().add("encoder", encoderClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                LoggerFactory.getLogger(this.getClass()).warn("error on create decoder cause by {0}", e.getCause());
            }

            try {
                if (!decoderClass.equals(Decoder.class))
                    definition.getPropertyValues().add("decoder", decoderClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                LoggerFactory.getLogger(this.getClass()).warn("error on create decoder cause by {0}", e.getCause());
            }

            boolean autoSetCookies = (Boolean) feignClientAnnotationAttr.getOrDefault("autoSetCookies", true);
            definition.getPropertyValues().add("autoSetCookies", autoSetCookies);
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
