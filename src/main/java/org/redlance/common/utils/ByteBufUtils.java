package org.redlance.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufUtils {
    /**
     * Faster than {@link dev.kosmx.playerAnim.core.util.MathHelper#readFromIStream(InputStream)} by a factor of two
     */
    public static ByteBuffer readFromIStream(InputStream stream) throws IOException {
        return ByteBuffer.wrap(stream.readAllBytes());
    }
}
