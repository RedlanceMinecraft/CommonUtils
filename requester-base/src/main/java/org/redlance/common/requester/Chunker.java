package org.redlance.common.requester;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.function.Function;

@FunctionalInterface
public interface Chunker<R, T> extends Function<T, R> {
    /**
     * Processes paginators
     * @param t Objects on one page
     * @return A result to stop at, or null to go on to the next page
     */
    @Override
    @Nullable
    R apply(T t);

    /**
     * Sends chunking request
     *
     * @deprecated Pass the client to send with: {@link #sendChunkingRequest(Methanol, Chunker, Function, TypeRef)}.
     */
    @Deprecated
    static <T, P, V extends Paginator<P>> T sendChunkingRequest(Chunker<T, P> chunker, Function<V, HttpRequest> builder, TypeRef<V> type) throws IOException, InterruptedException {
        return sendChunkingRequest(Requester.HTTP_CLIENT, chunker, builder, type);
    }

    /**
     * Sends chunking request
     *
     * @param client Client to send with.
     * @throws IOException A page failed or came without content, see {@link Paginator#failure()}.
     */
    static <T, P, V extends Paginator<P>> T sendChunkingRequest(Methanol client, Chunker<T, P> chunker, Function<V, HttpRequest> builder, TypeRef<V> type) throws IOException, InterruptedException {
        V paginator = null;
        int offset = 0;

        while (true) {
            paginator = client.send(builder.apply(paginator), type).body();
            if (paginator == null) throw new IOException("The response has no page");
            // A page without content is no last page: an error the paginator does not know, like a gateway's
            if (paginator.error() != null || paginator.data() == null) throw paginator.failure();

            T ret = chunker.apply(paginator.data());
            if (ret != null) {
                return ret;
            }

            // A page that does not move the offset on would be requested again forever
            if (paginator.done() || paginator.offset() <= offset) {
                return null;
            }

            offset = paginator.offset();
        }
    }
}
