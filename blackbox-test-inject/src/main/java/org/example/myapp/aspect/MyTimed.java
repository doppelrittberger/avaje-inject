package org.example.myapp.aspect;

import io.avaje.inject.aop.Aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Aspect(target = MyTimedAspect.class, ordering = 2000)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyTimed {

  String name() default "";
}
