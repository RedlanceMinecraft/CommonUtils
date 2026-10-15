package org.redlance.common.requester;

import com.github.mizosoft.methanol.MediaType;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.utils.CommonExecutors;
import org.redlance.common.utils.LambdaExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

public class RequesterUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger("Requester");

    public static boolean isTypeAccepted(String acceptHeader, @NotNull MediaType mediaType) {
        if (acceptHeader == null || acceptHeader.isBlank()) return false;
        for (String accept : acceptHeader.split(",")) {
            if (mediaType.includes(MediaType.parse(accept))) return true;
        }
        return false;
    }

    private static final ExecutorService PARALLEL_REQUESTER = CommonExecutors.createExecutor("parallel-requester-");

    @SuppressWarnings("unused")
    public static <R, T> Stream<R> prepareParallelRequests(Stream<T> requests, Function<? super T, R> mapper) {
        return requests.map(request -> PARALLEL_REQUESTER.submit( // TODO rewrite with java 26
                () -> mapper.apply(request)
        )).map(LambdaExceptionUtils.rethrowFunction((Future::get)));
    }
}
