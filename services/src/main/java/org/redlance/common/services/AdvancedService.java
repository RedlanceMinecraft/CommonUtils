package org.redlance.common.services;

public interface AdvancedService {
    boolean isActive();

    default int getPriority() {
        return ServiceUtils.DEFAULT_PRIORITY;
    }
}
