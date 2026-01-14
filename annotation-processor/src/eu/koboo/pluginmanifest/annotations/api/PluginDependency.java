package eu.koboo.pluginmanifest.annotations.api;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(PluginDependency.PluginDependencies.class)
public @interface PluginDependency {

    String pluginId();

    String version() default "*";

    DependencyType type() default DependencyType.REQUIRED;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface PluginDependencies {
        PluginDependency[] value();
    }
}
