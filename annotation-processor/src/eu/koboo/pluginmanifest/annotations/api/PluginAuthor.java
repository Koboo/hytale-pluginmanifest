package eu.koboo.pluginmanifest.annotations.api;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(PluginAuthor.PluginAuthors.class)
public @interface PluginAuthor {

    String name();

    String email() default "";

    String url() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface PluginAuthors {
        PluginAuthor[] value();
    }
}
