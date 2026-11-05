# requester-openpanel

Server-side delivery to OpenPanel's `POST /track` (`track`, `identify`, `increment`, `ai`) for a server with many
visitors: each call can forward its visitor's IP and User-Agent. Built on `requester-base` (`HttpClients`,
`RequesterUtils.LOGGER`) and Guava's `Cache`.

## Layout

- `OpenPanelRequester` — the requester. Constructors take `clientId`, `clientSecret`, `apiUrl` (default
  `DEFAULT_API_URL`, `https://api.openpanel.dev`; trimmed, trailing slashes cut, `/track` appended), the executor that
  runs `OpenPanelProfileSupplier`s (default: the client's own virtual-thread pool, so a supplier may block) and
  `maxInFlight` (default 64). A blank id or secret disables it: `enabled()` is false and every call completes at once
  without a request.
- Calls: `track`, `identify`, `increment` and `ai`, each as `(payload)` and `(payload, clientIp, userAgent)`, and all
  but `identify` as `(payload, profileSupplier, clientIp, userAgent)`. They return a `CompletableFuture<Void>` and never
  throw: a null payload, a rejection and a delivery failure all fail the future.
- `payload/` — `TrackPayload`, `IdentifyPayload`, `IncrementPayload`, and `AiPayload` (with `Kind`, a `Builder` and the
  factories `generation`, `embedding`, `span`, `trace`), `AiUsage`, `AiError`.
- `OpenPanelRequestException` — a response of 300 or above, with `statusCode()` and `responseBody()`; only such
  responses have their body read.

## Delivery

- `HTTP_CLIENT` is `HttpClients.newBuilder("http-openpanel")` with User-Agent `common-utils-requester-openpanel/1.0.0`
  and no disk cache (Methanol caches only GET, and every request here is a POST). The user chose the fixed `1.0.0`;
  don't derive it from the build. A per-call `userAgent` replaces it.
- The body is `MutableRequest.POST(uri, {type, payload}, APPLICATION_JSON)`, and Methanol sets `Content-Type` from the
  media type. Headers: `openpanel-client-id`, `openpanel-client-secret`, and `x-client-ip` when given. Timeout 15 s, no
  retry: a retried event is a duplicate.
- At most `maxInFlight` operations run at once; more fail with `RejectedExecutionException`. `shutdown()` stops
  accepting (later calls fail with `IllegalStateException`) and completes once the accepted ones finish.
- Failures are logged through `RequesterUtils.LOGGER`: a warning at most once a minute, debug otherwise.

## Identification

- An event with a `profileId` (`track`, `increment`, `ai`, all through `sendIdentified`) given a supplier first waits
  for an identification of that profile: it joins the attempt in flight, or starts one unless the profile was
  identified within 6 h or an attempt started within the 5-minute cooldown. The supplier returns the full
  `IdentifyPayload` with the event's `profileId`, or `null` to skip.
- Up to 100,000 identified profiles are remembered, and as many cooldowns. A failed identify never stops its event.
- `identify(...)` sends at once, starts the cooldown, and on success marks the profile identified.

## OpenPanel server facts

Checked in November 2026 against the self-hosted server's OpenAPI (`https://analytics.redlance.org/api/openapi.json`),
the docs (`https://openpanel.dev/docs/ai-observability/manual-capture`) and the sources of `Openpanel-dev/openpanel`
on GitHub: `packages/core/src/modules/ai/ai.constants.ts`, `packages/core/src/modules/ai/src/normalize.ts`,
`packages/core/src/modules/ingest/ingest.service.ts`, `packages/core/src/modules/ingest/src/incoming-event-handler.ts`,
`packages/core/src/modules/ingest/src/bots/suspicion.ts`, `packages/shared/src/server/parser-user-agent.ts` and the
Node SDK's `packages/sdks/sdk/src/index.ts`.

- `/track` takes `{type, payload}`, `type` being `track`, `identify`, `increment`, `decrement`, `alias`, `replay`,
  `group`, `assign_group` or `ai`. Requests with the client secret are never rejected or marked as bots.
- The OpenAPI JSON schema leaves out zod refinements: `zAiPayload.superRefine` rejects a `generation` or `embedding`
  without `model`, `provider` or `usage.inputTokens`, a `generation` without `usage.outputTokens`, and a `parentId`
  equal to the `spanId`. `AiPayload` checks all but the `parentId` rule, which, like the id patterns and lengths, only
  the server checks.
- AI events: ids take up to 128 of `A-Za-z0-9._:@()!'|~-`, names up to 200 characters; `input`, `output`, `tools`
  and `error` are cut at 200,000 bytes; custom `properties` lose keys starting with `ai_` and keep the first 100. They
  are stored as `ai_generation`, `ai_embedding`, `ai_span` and `ai_trace` and handled as server events whatever their
  User-Agent: they never open a session, and ride the live session of their `profileId` if there is one, taking its
  device id and device. Otherwise they have no device id unless the payload names a `deviceId`, and their device, OS
  and browser come from the User-Agent. Per the docs their content expires after 30 days, and a model missing from
  the price catalog costs 0 unless `cost` is sent.
- User-Agent: OpenPanel reads it as the visitor's device. One matching `/^[^/]+\/[\d.]+$/`, or one in which ua-parser
  finds no OS, browser, vendor or model, is a server (`device=server`), whose events ride an existing session instead
  of opening one; any other gives the device. For non-AI events it is also hashed with the IP into the device id, so a
  browser UA on events sent from a server makes up a visitor with sessions of its own.
- The Node SDK names itself in `openpanel-sdk-name`/`openpanel-sdk-version` headers, stored as the event's
  `sdkName`/`sdkVersion`; this requester sends neither.

## Payload conventions

- Records check only that what the server requires is present, not its lengths or patterns, and deep-copy their JSON
  nodes: an event that waits for an identification is serialized only when that finishes, on another thread.
- `JacksonMappers` leaves out null and empty values, `""`, `{}` and `[]` included, while `0` and `false` are sent. A
  component whose empty value is data takes `@JsonInclude(NON_NULL)`: `AiPayload.input`/`output`/`tools` and
  `AiError.message`, which OpenPanel requires but accepts empty.
- `AiPayload.Kind` is written lowercase through `@JsonProperty` on its constants.
- `AiPayload.Builder` exists because Kotlin cannot use named arguments on the 25-component Java constructor. It takes
  primitives for numbers and booleans, so `latencyMs(1840)` compiles in Java.

## Tests

- `./gradlew :requester-openpanel:test`: `OpenPanelRequesterTest` serves `/track` from a local `HttpServer` and checks
  the bodies and headers that arrive; `payload/AiPayloadTest` checks the per-kind validation. There are no live tests.

## Consumers

- EmotecraftLibrary: `server/.../analytics/AnalyticsService.kt` (the 5-argument constructor with `Dispatchers.IO` and
  128 in flight), its event policy in `ANALYTICS.MD`. It pins a published release, whose delivery `ANALYTICS.MD` and
  the `SERVER_USER_AGENT` comment in `AnalyticsService.kt` describe: update them when the consumer moves to a release
  that changes delivery here.
