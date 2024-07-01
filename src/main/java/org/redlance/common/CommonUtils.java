package org.redlance.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonUtils {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static final Logger LOGGER = LogManager.getLogger("Redlance CommonUtils");

    public static void main(String... args) { // Used for testing things
    }
}
