package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.adapter.ForwardingDecoder;
import com.github.mizosoft.methanol.adapter.ForwardingEncoder;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import io.github.kosmx.emotes.server.config.Serializer;

public class GsonAdapters {
    public static class Encoder extends ForwardingEncoder {
        public Encoder() {
            super(GsonAdapterFactory.createEncoder(Serializer.serializer));
        }
    }

    public static class Decoder extends ForwardingDecoder {
        public Decoder() {
            super(GsonAdapterFactory.createDecoder(Serializer.serializer));
        }
    }
}
