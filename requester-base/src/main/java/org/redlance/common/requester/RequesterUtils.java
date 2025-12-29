package org.redlance.common.requester;

import com.github.mizosoft.methanol.MediaType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequesterUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger("Requester");

    public static boolean isTypeAccepted(String acceptHeader, @NotNull MediaType mediaType) {
        if (acceptHeader == null || acceptHeader.isBlank()) return false;
        for (String accept : acceptHeader.split(",")) {
            if (mediaType.includes(MediaType.parse(accept))) return true;
        }
        return false;
    }
}
