package eu.koboo.pluginmanifest.gradle.plugin.utils;

import lombok.experimental.UtilityClass;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

@UtilityClass
public class PluginLog {

    private static final String PREFIX = "> PluginManifest: ";
    private static final Logger LOGGER = Logging.getLogger(PluginLog.class);

    public void info(String message) {
        print(PREFIX + message);
    }

    public void print(String message) {
        LOGGER.lifecycle(message);
    }
}
