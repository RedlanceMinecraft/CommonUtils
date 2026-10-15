# requester-base

The HTTP foundation of the requester modules: shared Methanol client settings and on-disk cache, offset paging
(`Chunker`/`Paginator`), a downloader and a per-host proxy selector. Built on Methanol 1.9 (with its Jackson 3 adapter)
and this repo's `common` (`CommonExecutors`), `jackson` (`JacksonMappers`) and `reflect` (`LambdaExceptionUtils`); these
and slf4j-api are `api` dependencies, so consumers get them too.

## Layout

- `HttpClients` — what every client shares. `newBuilder(name)` returns a `Methanol.Builder` with a new executor of its
  own (virtual-thread cached pool, threads `<name>-0`, `<name>-1`, …), HTTP/1.1, a 1-minute connect timeout,
  `UrlProxySelector.INSTANCE`, `Redirect.ALWAYS`, one shared in-memory `CookieManager`, a browser user agent and the
  codec: JSON through `JacksonMappers.OBJECT_MAPPER`, Smile through `SMILE_MAPPER` for `APPLICATION_JACKSON_SMILE`
  (`application/x-jackson-smile`). It caches nothing.
- `HttpClients.CACHE` — the one Methanol `HttpCache` for the clients that opt in with `.cache(HttpClients.CACHE)`:
  `httpcache/` under the JVM working directory, 1 GiB, closed by the `http-cache-saver` shutdown hook. `Requester` and
  `Downloader` add it; requester-boosty does not, as its answers hold personal data.
- `interceptors/CacheOverrideInterceptor` — workaround for methanol#94: on a successful response it replaces
  `Cache-Control` with `max-age=<max(server max-age, 15)>`, dropping `no-store`, `private` and the rest, so the response
  is cacheable for at least 15 s. Add it with `backendInterceptor(...)`: backend interceptors see only network
  responses, before the cache stores them.
- `Downloader` — `download(uri[, listener])` through its own client `"http-downloader"` with `CacheOverrideInterceptor`;
  progress goes through the public `TRACKER` (1 MiB steps) and is logged by default. `downloadImage(uri[, w, h])`
  (default 256×256) returns a PNG scaled to `min(w, original width) × min(h, original height)`: never upscaled, aspect
  ratio not kept.
- `UrlProxySelector.INSTANCE` — the proxy selector of every client. `registerDomainPredicate(domain, …)` matches when
  the host *contains* `domain`; `registerCustomPredicate(predicate, …)` takes any test; an `InetSocketAddress` means an
  HTTP proxy. Every matching proxy is returned, none means direct. It is a plain `HashMap`, so register before requests
  start. Nothing in this repo registers one.
- `RequesterUtils` — `LOGGER` (slf4j `"Requester"`, used by requester-boosty, -mojang and -openpanel), `isTypeAccepted`,
  `prepareParallelRequests` (see Gotchas).
- `Chunker`, `Paginator` — offset paging (see Paging); `Requester` — deprecated (see Deprecated).
- `DisallowedHeadersCleaner`, `JavaLinkVisitor`, `interceptors/CookieFixerInterceptor` and
  `interceptors/FallbackInterceptor` are commented out in full (old package `org.redlance.common.utils.requester`) and
  not compiled.

## Conventions

- Each requester builds one client of its own in a `static final` field from `HttpClients.newBuilder("http-<name>")`
  and adds only what it needs. The user wants a separate executor for every client: don't share a client between
  requesters (each `newBuilder` call creates a new executor).
- Add `.cache(HttpClients.CACHE)` only where responses may sit on disk (never for personal data), and
  `CacheOverrideInterceptor` on top only where answers up to 15 s old are fine. requester-boosty (`"http-boosty"`)
  adds neither: every Boosty answer is the current state and holds personal data.
- The browser user agent (`Chrome/155.0.0.0`) was set deliberately by the user; keep it. It is a default header, so a
  request that sets its own `User-Agent` keeps that one (requester-openpanel forwards the end user's).
- No request timeout is set, only the connect timeout: give requests `.timeout(...)` (requester-openpanel uses 15 s).
- Let the codec map bodies: `client.send(request, Type.class or TypeRef)`, `MutableRequest.POST(object, mediaType)`.
  `JacksonMappers` ignores unknown properties and leaves null and empty values out when encoding.

## Paging

- `Chunker.sendChunkingRequest(client, chunker, requestFactory, typeRef)`: the factory gets the previous page (`null`
  for the first) and builds the next request from its `offset()`; the chunker gets each page's `data()`. A non-null
  chunker result stops the walk and is returned; `null` is returned after the last page.
- The walk ends on `done()` or when `offset()` does not exceed the previous one (starting at 0), so `offset()` must be
  the offset of the NEXT page.
- The HTTP status is not checked. No page (`null` body) throws `IOException("The response has no page")`; a page with an
  `error()` or without `data()` throws its `failure()`, so error answers must decode into the paginator type.
  `failure()` defaults to a plain `IOException`; requester-boosty's paginators override it with their own exception.

## Deprecated

- `Requester` (not for removal). Its static `HTTP_CLIENT` is `HttpClients.newBuilder("http-requester")` plus
  `CacheOverrideInterceptor`. Each member's `@deprecated` Javadoc names its replacement: a client of your own with
  `send`/`sendAsync`, `HttpClients.CACHE.remove(request)`, `MutableRequest.POST(object, mediaType)`.
- Still used by requester-mojang (`MojangRequester` calls `Requester.sendRequest`) and requester-openpanel
  (`OpenPanelRequester` uses `HTTP_CLIENT`, its executor as the default profile-lookup executor, and `ofObject`).
- `Chunker.sendChunkingRequest(chunker, builder, type)` without a client sends through `Requester.HTTP_CLIENT`, so its
  pages are force-cached; pass a client.

## Gotchas

- `prepareParallelRequests(stream, mapper)` (moved here from `Requester`) submits to the shared `parallel-requester-`
  executor, but on a sequential stream each `Future.get` blocks before the next element is submitted, so requests run
  one at a time. `ExecutionException`/`InterruptedException` are thrown undeclared. No live caller: requester-mojang's
  `NameHistoryRequester` is commented out and still calls `Requester.prepareParallelRequests`.
- `isTypeAccepted(accept, type)` is true only when `type` includes an entry of the header, so a wildcard entry never
  matches a concrete type (`*/*` does not accept JSON; `ServerUtilsTest` pins this). Entries are not trimmed:
  `text/html, */*` throws `IllegalArgumentException` once the scan reaches ` */*`.
- `Downloader.download` throws `IllegalStateException` (message: the status code) for anything but 200.
- `httpcache/` holds response bodies (git-ignored): don't commit or dump it. Don't open another `HttpCache` on it; per
  the Javadoc the directory takes one store at a time.

## Tests

- `./gradlew :requester-base:test` runs `ServerUtilsTest` (`isTypeAccepted` only); there are no live tests. Paging is
  tested in requester-boosty (`BoostyPagingTest`, pages from a local `HttpServer`), so after changing `Chunker`,
  `Paginator` or `HttpClients` run `./gradlew :requester-boosty:test` too.
- Tests run in `build/test-workdir`, so the cache lands in `build/test-workdir/httpcache`.

## Consumers

- requester-boosty (`HttpClients`, `Chunker`, `Paginator`), requester-mojang (`Requester`, `Downloader`),
  requester-openpanel (`Requester`), and `all`, which re-exports every module. requester-bypass does not use it.
