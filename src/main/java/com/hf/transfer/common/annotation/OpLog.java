package com.hf.transfer.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    String logType() default "APPLICATION";

    String bizType() default "";

    String module() default "";

    String desc() default "";

    boolean saveParams() default true;

    boolean saveResult() default true;
}
