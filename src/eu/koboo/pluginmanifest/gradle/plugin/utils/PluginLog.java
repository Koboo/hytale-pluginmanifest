package eu.koboo.pluginmanifest.gradle.plugin.utils;

import lombok.experimental.UtilityClass;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

@UtilityClass
public class PluginLog {

    public static final Logger LOGGER = Logging.getLogger(PluginLog.class);

    public void info(String message) {
        LOGGER.lifecycle("> PluginManifest: " + message);
    }
}
