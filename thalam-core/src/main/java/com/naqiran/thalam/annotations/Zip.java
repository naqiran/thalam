package com.naqiran.thalam.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 
 * @author Nakkeeran Annamalai
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AggregatorLifeCycle(type = LifeCyleMethodType.ZIP)
public @interface Zip {
    @AliasFor(annotation = AggregatorLifeCycle.class, attribute = "service")
    String service();
    @AliasFor(annotation = AggregatorLifeCycle.class, attribute = "version")
    String version();
}