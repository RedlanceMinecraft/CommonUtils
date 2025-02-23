package org.redlance.common.emotecraft.services;

import io.github.kosmx.emotes.api.services.LoggerService;
import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.common.tools.ServiceLoaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.jul.LevelTranslator;

import java.util.logging.Level;

public class Logger4jService implements LoggerService {
    protected static final Logger LOGGER = LogManager.getLogger(CommonData.MOD_NAME);

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        Logger4jService.LOGGER.log(LevelTranslator.toLevel(level), msg, throwable);
    }

    @Override
    public void log(Level level, String msg) {
        Logger4jService.LOGGER.log(LevelTranslator.toLevel(level), msg);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public int getPriority() {
        return ServiceLoaderUtil.HIGHEST_SYSTEM_PRIORITY;
    }
}
