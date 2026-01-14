package eu.koboo.pluginmanifest.annotations.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginManifest {

    String group();

    String name();

    String version();

    String description() default "";

    PluginAuthor[] authors();

    String website() default "";

    PluginDependency[] dependencies() default {};

    boolean disabledByDefault() default false;

    boolean includesAssetPack() default false;
}
