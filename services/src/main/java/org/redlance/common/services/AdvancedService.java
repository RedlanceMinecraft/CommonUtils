package org.redlance.common.services;

public interface AdvancedService {
    boolean isServiceActive();

    default int getPriority() {
        return ServiceUtils.DEFAULT_PRIORITY;
    }
}
