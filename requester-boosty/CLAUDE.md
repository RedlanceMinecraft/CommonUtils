# requester-boosty

Wrapper of the Boosty API (boosty.to) on behalf of a blog owner: subscribers, post sales, donations, subscriber
profiles, dialogs, the pubsub websocket and token refresh. Built on `requester-base` (`HttpClients`, `Chunker`, `Paginator`) and `cache`.

## Layout

- `BoostyRequester` — static requests. `HTTP_CLIENT` is package-private (`HttpClients.newBuilder("http-boosty")`
  without the disk cache: every Boosty answer is the current state and holds personal data, so nothing may land in
  `httpcache/`; `BoostyPagingTest` pins that); `BoostySessionStorage` and the websocket use it too. Every request
  carries `TIMEOUT` (30 s), as the client only has a connect timeout. Paged requests go through
  `Chunker.sendChunkingRequest(HTTP_CLIENT, …)`: a non-null chunk result stops the walk and is returned, `null` after
  the last page. The list-returning `requestPostSales`/`requestSubscribers` are deprecated for removal.
- `BoostyErrorResponse` (`error`, `error_description`, `failure(...)`) is implemented by every response envelope and
  paginator; `BoostyRequestException extends IOException` carries `error()`/`errorDescription()`.
- `paginators/` — `BoostyLegacyPaginator` (subscriber list: `data, limit, offset, total`) and `BoostyPaginator` (post
  sales and donations: `data, extra{offset, total, isLast}`).
- `obj/user` — `BoostyUser` (one flat record for list rows, profiles, post buyers and donors), `SubscriptionStatus`,
  `SubscriptionLevel`, `Currency`, `BoostyPromo`, `ExternalApp`, `BoostyProfileDeserializer`.
- `obj/post` — `PostSales`, `PostSale` (`double amount`), `PostObj`, `ContentBlock`.
- `obj/donation` — `Donation` (`double amount`, `type`, `targetId`). Its pages are the package-private
  `BoostyRequester.Donations`, and `requestDonationsChunking` hands the chunker the list itself.
- `obj/BoostyValues` (`@ApiStatus.Internal`: `parse(enum)`, `nullIfEmpty`), `obj/LenientDeserializer`.
- `BoostyWebsocketListener` — the pubsub connection; `messages/` — its protocol records; `handler/BoostyBuiltInHandler`
  implements `BoostyWebsocketListener.Listener` and hands events to `BoostyListener`s by event type.
- `BoostySessionStorage` — access/refresh tokens in `<root>/boosty-<deviceId>.json`, refreshed through
  `POST https://api.boosty.to/oauth/token/` (form: `device_id`, `device_os=web`, `grant_type=refresh_token`,
  `refresh_token`) on construction and then daily.

## Boosty API facts (checked against the live API in October 2026)

- Every request: `Authorization: Bearer <access token>`. Errors come as `{"error", "error_description"}`: 401
  `unauthorized`, 403 `forbidden`, 404 `blog_not_found` / `user_not_found`.
- Subscribers: `GET /v1/blog/<blog>/subscribers?sort_by=on_time&limit=300&status=active[&level_ids=a,b]&offset=N&order=gt`.
  - `is_active=true` is silently ignored; filter with `status=active|inactive|paused`.
  - `order=gt` is newest first, `lt` oldest first; `sort_by` is `on_time` or `payments`.
  - `level_ids` is exact: subscribers of an archived version of a level are not returned for the current one.
  - The response `offset` is already the offset of the NEXT page. Offset paging shifts when a subscription starts or
    ends mid-walk (a row may repeat or be missed until the next walk).
- Post sales: `GET /v1/blog/<blog>/sales/post/?limit=300&offset=N&sort_by=time&order=gt`; `extra.offset` is a JSON
  string.
- Donations: `GET /v1/blog/<blog>/sales/donation/?limit=300&offset=N&sort_by=time&order=gt[&type=…][&target_id=…]` →
  `{"data": {"donationsSales": [{id, type, targetId, amount, isFeePaid, createdAt, bloggerId, user}]},
  "extra": {offset, total}}`, with `extra.offset` a string (requested offset plus limit) and no `isLast`.
  - `type` is `author` (from the profile), `target` (a goal), `post` or `dialog`, as in the frontend's enum. An
    unknown `type` or a `limit` above 300 gets 400 `invalid_param`; `target_id` filters alone or with `type`.
  - `targetId` is the goal for `target` and `0` for `author`; the frontend lists a post's donations with
    `type=post&target_id=<post id>`. The blog has only `author` and `target` donations, so `post`/`dialog` rows are
    unseen.
  - Amounts are rubles after Boosty's commission, as the statistics page states for every payment; `user` is the
    donor's identity like a post buyer's. Goals themselves (`/v1/target/<blog>/`, `/v1/target/<id>`) are not wrapped.
- Profile: `GET /v1/blog/<blog>/subscriber/<userId>/profile` → `{"data": {"profile": {...}}}` with `user`, `note`,
  `canWrite`, `isBlackListed`, `isBlackListedByUser`, `finance{payments, refunds}` and `subscription{subscriptionLevel,
  status, onTime, offTime, nextPayTime, lastPayTime}`. Works for any user id; `subscription` is absent for users who
  never subscribed. Profile statuses: `active`, `trial`, `renewal_canceled`, `paused`, `expired`.
- Dialog: `GET /v1/dialog/<id>` (only `chatmate.id` is read). Socket token: `GET /v1/ws/connect` → `{"token"}`.
- Currencies (Boosty frontend enum): RUB, EUR, USD, UAH, BRL, CHF, SEK, GBP, AED, AUD, JPY, UZS, KZT. Money in
  `BoostyUser` is in rubles, times are Unix seconds.
- External apps: `discord` is the one actually linked by subscribers; `telegram` and `max` exist but none were seen.
- Blog `dima_dencep` levels: free 421849, "Beta versions" 2452741 (150 ₽), "Supporter" 4126895 (399 ₽), archived
  "Beta" 2310700 (125 ₽, `parent` 2452741). EmotecraftLibrary accepts `boostyLevelIds: [2452741]`.

## More endpoints (from the frontend's API map of 322 entries, not wrapped)

- `GET /v1/blog/stat/<blog>/events?limit=…&offset=…` — the statistics page's event feed, the latest first, paged by
  `extra{offset (string), total, isLast}`. Each row names the `user` (with e-mail) and carries `eventType`, `amount`,
  `createdAt`, `levelId`/`oldLevelId`, `period`, `isFeePaid`. Types: `subscribe`, `recurrent`, `unsubscribe`,
  `unsubscribe_cancel`, `up_subscription`, `donation_author`/`donation_target`/`donation_post`/`donation_dialog`,
  `buy_post`, `buy_message`, `buy_bundle`, `buy_gift`, `get_gift`, `hold`, `hold_approve`, `hold_cancel`, `refund`,
  `payout`, `payout_error`, `commission_compensation`, `referal`, `referal_out`. Unlike the websocket counters it says
  who did what, so a sync can read it from the last seen event instead of walking every subscriber.
- `GET /v1/blog/stat/<blog>/search` finds users in the blog's statistics (parameters unchecked);
  `GET /v1/blog/stat/<blog>/current` gives balance, income, hold, `paidCount` and `followersCount`.
- `GET /v1/blog/<blog>/subscription_level/?show_deleted=true&show_free_level=true` lists every level, archived ones
  included.
- `GET /v1/notification/standalone/event/` lists the notifications behind the `standalone_notify_count` counters,
  with the subscriber and level of a `subscriber_new` and the donation (with `currencyAmounts`) of a `donation_new`.
- Boosty runs an OAuth server and developer apps (`/oauth/server/*`, `/client/register`, `/dev/v1/user/token`, docs
  under `/doc/*`). They are gated: this account has `hasAccessToDevApi: false` and `/v1/doc/index/` answers 403 until
  access is requested through `/doc/request_access/`.
- `/int/self/*` (users, sessions, mailer, admin actions, …) belongs to Boosty staff: never call it.

## Websocket

- `wss://pubsub.boosty.to/connection/websocket` with `Origin: https://boosty.to`; Centrifugo-style JSON commands
  `{"id", "method", "params"}`: 0 connect `{"name": "js", "token": <socket token>}`, 1 subscribe `{"channel"}`,
  2 unsubscribe, 7 ping. Replies are `{"id", "result"}` or `{"id", "error"}`; publications have no id:
  `{"result": {"channel", "data": {"data": {"type", "data"}}}}`.
- One frame may hold several newline-separated messages, and one message may span several frames.
- Channels: `users#<id>`, `dialogs#<id>` and `blogger:<id>`, the blog's public channel that `GET /v1/blog/<blog>`
  names as `publicWebSocketChannel`. The web frontend never subscribes to `blogger:` and nothing was seen on it, but
  keep it: it is a real public namespace, and `blog_subscriber_stat`, which EmotecraftLibrary acts on, may come there.
- `WEBSOCKET.md` holds the full shape of every event on every channel, each marked with where it comes from (the
  frontend's handlers or the REST object it stores). Keep it updated when a live push shows more.
- Per event, Boosty nests the payload (`event.data.data`) or puts its fields next to `type` (`event.data`);
  `BoostyListener.payload(event)` takes whichever is there. No event announces a donation: the `donation_new`
  counter of `standalone_notify_count` grows instead.
- The JDK `WebSocket` fails `sendText` while a previous send is pending, so sends are chained per connection.
- `BoostyWebsocketListener` is final and composition-only: `new BoostyWebsocketListener(socketTokenSupplier, userId,
  listener)`, `start()`, `close()`, `isConnected()`, `sendMessage(method, params)`. `Listener` has
  `onEvent(channel, event)`, `onConnected(reconnected)` (`true` means events were missed, so resync) and
  `onDisconnected(cause)`; they run on the socket's threads and on the scheduler every listener shares, so they must
  return quickly; their exceptions are only logged.
- It keeps itself alive, so consumers need no subclass or watchdog (the user asked for exactly this; keep it so):
  a connection counts as up only after auth and every subscription are answered, and only the up connection delivers
  events; ping every 30 s, a command unanswered for 15 s fails and an unanswered ping drops the connection; after a
  drop it reconnects at once (after 30 s if the connection lived under 30 s); a failed connect retries every 30 s.
- A connect first takes the socket token (the supplier blocks, so it runs on its own virtual-thread executor), then
  opens the socket (30 s timeout), so no socket idles unauthorized. Command timeouts run on the listener's scheduler,
  not through `orTimeout`, whose single JDK-wide thread would then run `lost()` and the listener callbacks.
- `close()` may land while a socket is still opening; `Connection.opened(socket)` aborts it once it arrives.
- Tests drive it through the package-private constructor with an `opener` and a short `Timing`.

## Data model conventions

- Records with final components and full Javadoc (`@param` for each component), JetBrains `@Nullable`.
- Jackson 3: `tools.jackson.*` databind with `com.fasterxml.jackson.annotation.*` annotations, mapper
  `JacksonMappers.OBJECT_MAPPER`. Boosty fields are camelCase except the error envelope.
- Empty strings become `null`; missing collections become empty unmodifiable ones.
- Enums carry `UNKNOWN` and a `@JsonCreator of(String)`, so a new Boosty value never fails a page.
  `SubscriptionStatus` is a single enum with an `isActive()` flag: ACTIVE, TRIAL, RENEWAL_CANCELED give access; PAUSED,
  INACTIVE, EXPIRED, SUSPENDED, UNKNOWN do not.
- `LenientDeserializer` sits on fields whose shape Boosty varies (promo, external apps, currency prices, level
  parent/data): bad list elements and map entries are dropped, anything else becomes `null`.
- Records keep the default value equality (the user chose it); compare `id()` for identity. Don't bring back id-only
  `equals`.
- There is no `BoostyProfile`: the profile is flattened into `BoostyUser` by `BoostyProfileDeserializer` (a whitelist;
  no `user` object → `null`).
- `BoostyUser.isSubscribed()` trusts the status first, then `nextPayTime`/`offTime` with a day of grace; a post buyer
  is not subscribed. `isPaidSubscriber(levelIds...)` also accepts archived levels by walking `SubscriptionLevel.parent()`,
  which only the subscriber list sends: a profile's level is short and has no parent.

## Tests

- `./gradlew :requester-boosty:test` (working dir `build/test-workdir`).
- `BoostySchemaTest` parses fixtures from `src/test/resources/boosty/*.json`; `BoostyPagingTest` serves pages from a
  local `HttpServer`; `BoostyWebsocketListenerTest` uses a `FakeWebSocket` that, like the JDK, rejects concurrent sends
  (wait for sends with `awaitSent`, reading `sent.getLast()` races); `tests/BoostyBuiltInHandlerTest`.
- `tests/BoostyRequesterTest` is live and read-only (blog `dima_dencep`, level 2452741); it runs only with
  `-Dboosty.token=<access token>`, which the build forwards to the test JVM. The user supplies the token.

## Privacy

- Boosty responses hold subscribers' e-mails, names, notes and payments. When you handle real ones (research, live
  tests), never dump, persist or commit them and report only shapes and aggregate counts; fixtures are synthetic, and
  live test messages name users by id only.
- Never print, log or store an access token.

## Researching the API

- Use Claude in Chrome on `https://boosty.to/<blog>/blog/statistics` (tabs "Подписчики", "Донаты" and "Продажи
  постов"; a subscriber's profile opens by clicking their name) and read the network requests. Pages are rendered on
  the server, so the first view may come without API calls; navigate inside the app to trigger them, or list what the
  page already loaded with `performance.getEntriesByType('resource')`.
- For ad-hoc GETs, read the page's own token from `localStorage.auth` inside `javascript_tool` and use it there only.
- The frontend's scripts (fetch them in-page and search the text) hold the API path map (`BLOG_DONATIONS_SALES:
  "/blog/:blogUrl/sales/donation/"`), the parameters every fetch sends, and the enums of event types and kinds. The
  websocket service is the class with `static subscribe(` and `register({channel:…,message:…,handler:…})` calls.
- A websocket opened in the page (token from `/v1/ws/connect`, channels from `/v1/user/current` → `webSocket`) shows
  real pushes; record their shapes only. `history` answers with no publications.
- The extension withholds output that looks like a query string, a token or a "sensitive" key (even a key named
  `author`): print paths, parameter names, shapes and counts instead.

## Consumers

- EmotecraftLibrary (`../EmotecraftLibrary`, module `server`, `data/supporters/`): `SupporterService.kt` walks
  subscribers and checks `isSubscribed`/`isPaidSubscriber`; `BoostyRealtimeSync.kt` listens to the websocket (its
  `SupervisedSocket` and watchdog are obsolete since the listener reconnects itself). Check it with
  `cd ../EmotecraftLibrary && ./gradlew :server:compileKotlin --rerun --include-build ../CommonUtils --offline`.
- enhanced-player-animations (`~/Desktop/git/dima-dencep/enhanced-player-animations`) is pinned to 1.3.9 and relies on
  the old semantics (post buyers counted as subscribed, `getKeyByValue(user)` with id-only equality). The user ports it.
