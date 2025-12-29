package org.redlance.common.requester;

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
     * @return null if execution should be terminated
     */
    @Override
    @Nullable
    R apply(T t);

    /**
     * Sends chunking request
     */
    static <T, P, V extends Paginator<P>> T sendChunkingRequest(Chunker<T, P> chunker, Function<V, HttpRequest> builder, TypeRef<V> type) throws IOException, InterruptedException {
        V paginator = null;

        while (true) {
            paginator = Requester.sendRequest(builder.apply(paginator), type);
            if (paginator.error() != null) throw new InterruptedException(paginator.error());

            T ret = chunker.apply(paginator.data());
            if (ret != null) {
                return ret;
            }

            if (paginator.done()) {
                return null;
            }
        }
    }
}
