package com.naqiran.thalam.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregatorLifeCycle {
    LifeCyleMethodType type() default LifeCyleMethodType.NOTHING;
    String service() default "";
    String version() default "";
}
