package edu.spsu.aspectjava.weaver.models.example;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AspectAction {
	String value();
}
