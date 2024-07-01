package org.redlance.common.emotecraft.utils;

import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.executor.Logger;
import io.github.kosmx.emotes.executor.dataTypes.IClientMethods;
import io.github.kosmx.emotes.executor.dataTypes.IDefaultTypes;
import io.github.kosmx.emotes.executor.dataTypes.IGetters;
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
    public IDefaultTypes getDefaults() {
        return null;
    }

    @Override
    public IGetters getGetters() {
        return null;
    }

    @Override
    public IClientMethods getClientMethods() {
        return null;
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
    public void writeLog(Level level, String msg) {
        CommonUtils.LOGGER.log(LevelTranslator.toLevel(level), msg);
    }
}
