package org.redlance.common.requester.openpanel;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.openpanel.payload.IdentifyPayload;

/**
 * Lazily creates the complete profile used for automatic identification.
 *
 * <p>The supplier runs outside the caller's thread and may capture any domain-specific
 * context needed to build the profile.</p>
 */
@FunctionalInterface
public interface OpenPanelProfileSupplier {
    @Nullable IdentifyPayload get() throws Exception;
}
