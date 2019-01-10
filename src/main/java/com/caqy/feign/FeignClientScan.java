package com.caqy.feign;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(FeignClientScannerRegistrar.class)
public @interface FeignClientScan {
    String[] value() default {};

    String[] basePackages() default {};

    Class<?> markerInterface() default Class.class;

    String feignBuilderRef() default "";
}
