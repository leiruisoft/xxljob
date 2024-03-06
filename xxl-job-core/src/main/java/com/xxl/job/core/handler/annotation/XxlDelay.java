package com.xxl.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * @author helei
 * @description
 * @since 2023-07-10 16:21
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlDelay {
    /**
     * jobhandler name
     */
    String value();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";
}
