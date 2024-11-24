package org.redlance.common.emotecraft.utils;

import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.executor.Logger;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.redlance.common.CommonUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class EmoteInstanceImpl extends EmoteInstance implements Logger {
    static {
        CommonUtils.LOGGER.debug("Initializing own emotecraft instance!");
    }

    @Override
    public Logger getLogger() {
        return this;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public Path getGameDirectory() {
        return Paths.get("");
    }

    @Override
    public Path getConfigPath() {
        return getGameDirectory().resolve("config.json");
    }

    @Override
    public void writeLog(Level level, String msg, Throwable throwable) {
        CommonUtils.LOGGER.log(LevelTranslator.toLevel(level), msg, throwable);
    }

    @Override
    public void writeLog(Level level, String msg) {
        CommonUtils.LOGGER.log(LevelTranslator.toLevel(level), msg);
    }
}
