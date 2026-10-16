# Boosty pubsub websocket

What `wss://pubsub.boosty.to/connection/websocket` sends, as of October 2026, with the full shape of every event.

Sources, marked on every event:

- **handler**: the fields the handler of Boosty's web frontend (`app.*.js` on `static.boosty.to`) reads; the types
  follow from how it uses them. Boosty may send more fields.
- **REST**: the frontend stores the payload whole in the same place as the object a REST endpoint returns, so the
  shape is that endpoint's, read from a live response.
- **unknown**: nothing in the frontend reads it and no live push was seen. During the research a connection
  subscribed to all three channels of the blog `dima_dencep` received no event at all, so no shape below comes from a
  live push.

## Protocol

- Centrifugo JSON protocol with numbered methods: `0` connect `{"name": "js", "token": <socket token>}`, `1` subscribe
  `{"channel"}`, `2` unsubscribe, `6` history, `7` ping. The frontend also publishes into a channel (typing in a
  dialog), reads presence of stream viewers, refreshes the connection through `GET /v1/ws/refresh` and gets tokens for
  private channels from `GET /v1/ws/subscribe`.
- Socket token: `GET /v1/ws/connect` → `{"token"}`. `GET /v1/user/current` names the user's channels in
  `webSocket.channel` and `webSocket.dialogs`, next to a `webSocket.token`; `GET /v1/blog/<blog>` names the blog's public
  channel in `publicWebSocketChannel`.
- Replies seen live: connect `{client, version}`; subscribing to `users#<id>` and `dialogs#<id>` gives
  `{epoch, offset, positioned, recoverable}`, but `history` returns no publications; `blogger:<id>` subscribes with
  `{}`. A channel of an unknown namespace gets error `102 unknown channel`, another user's `users#` gets
  `103 permission denied`, and any blog's `blogger:` subscribes, as it is public.
- A push has no `id`: `{"result": {"channel": "users#<id>", "data": <publication>}}`. The publication is what
  `BoostyWebsocketListener.Listener.onEvent(channel, event)` gets:

```ts
type Event = {
  data: Published;   // what Boosty published; every event below shows its Published
  offset?: number;   // Centrifugo's position in the channel
};
```

### Where the payload sits

Per event, Boosty either nests the payload or puts its fields next to `type`:

```ts
type Nested<T> = { type: string; data: T };             // payload: event.data.data
type Flat<T>   = { type: string } & T;                  // payload: event.data, type included
```

`BoostyListener.payload(event)` returns `event.data.data` when there is one and `event.data` otherwise, so a typed
`BoostyListener` reads both. `blog_verification_status` also carries `extra` next to `data`; read it from the raw
event.

Conventions below: ids and times are numbers, times in Unix seconds; `T?` may be missing.

## Channels

| Kind in the frontend | Channel | Name from | The frontend subscribes |
|---|---|---|---|
| `user` | `users#<user id>` | `webSocket.channel` | while logged in |
| `dialog` | `dialogs#<user id>` | `webSocket.dialogs` | on the message pages |
| — | `blogger:<owner id>` | `publicWebSocketChannel` of the blog | never |
| `custom` | one per dialog | the dialog's `wsChannel` | while a dialog is open |
| `chat`, `private` | a stream's chat | the stream | on a stream page |
| `stream` | a stream | the stream | on a stream page |
| `viewers` | a stream's presence | the stream | for viewer counts (presence only) |
| `global` | `global:public-stream` | fixed | never: its one handler is not registered |

`BoostyWebsocketListener` subscribes to `users#`, `dialogs#` and `blogger:` of the given user id.

## `users#<id>`

### `blog_subscriber_stat` — unknown

Listed among this channel's events, but the frontend registers no handler for it, and the 2024 and 2025 builds kept by
the Wayback Machine hold no websocket code at all, so only a live push or a log can show its payload. EmotecraftLibrary
walks the subscribers on it without reading its payload. Which channel brings it is unconfirmed too: it may be the
public `blogger:` one. `GET /v1/blog/stat/<blog>/current` returns the counters of the statistics page
(`paidCount`, `followersCount`, `income`, `balance`, `hold`, `payoutSum`, `commissionFee`, `hasReferralDiscount`); whether
this event carries them is unknown.

### `subscribers_change_count` — flat, handler

```ts
Flat<{ count: number }>   // the blog's subscriber count; the frontend stores it as blog.count.subscribers
```

### `standalone_notify_count` — flat, handler + REST

```ts
Flat<{
  counters_by_type: Array<{
    type: NotificationType;
    unread: number;   // the frontend sums these into the bell badge
    total: number;    // from the REST twin, GET /v1/notification/standalone/event/ → count.byEventType
  }>;
}>

type NotificationType =
  | "post_comment_new" | "post_comment_new_reply" | "post_new" | "post_buy" | "bundle_buy" | "project_news"
  | "video_stream_start" | "promo_new" | "donation_new" | "message_buy" | "referal_new_blogger" | "referal_new_user"
  | "subscriber_new" | "target_new" | "support_ticket_update" | "blog_stat_report_ok" | "blog_stat_report_failed"
  | "level_subscribers_limit_reached";
```

A donation raises the `donation_new` counter: no other event announces one. The snake_case `counters_by_type` is a
key nothing else in Boosty's camelCase API uses, so the frontend must have taken it from real pushes; whether its
entries carry `total` as well is unconfirmed. EmotecraftLibrary reads `event.data.data.byEventType`, modelled on the
REST twin, which most likely never matches.

### `dialog_message_counters` — nested, handler

```ts
Nested<{
  userUnreadCounter: number;     // unread messages of the user; the frontend stores it as user.unreadMsgCount
  dialogUnreadCounter: number;   // unread messages of the dialog
  dialogId: number;
}>
```

### `publish_post` — flat, handler

```ts
Flat<{ postId: string }>   // a post went public, e.g. a scheduled one
```

### `video_completed` — nested, handler

```ts
Nested<{
  id: string;   // key of the processing video
  containerType: "blog" | "level" | "post" | "comment" | "dialog_message" | "dialog_broadcast_message"
    | "subscription_thank_message" | "stream" | "stream_chat_message";
  containerId: string | number;   // id of the post, message, … that holds the video
  video: {                        // the video block as the container holds it
    id: string;
    status: string;
    complete: boolean;
    playerUrls?: Array<unknown>;
  };
}>
```

### `user_update` — nested, REST

```ts
Nested<Partial<CurrentUser>>   // merged into the current user
```

`CurrentUser` is `GET /v1/user/current`, with these fields among a few more: `id`, `name`, `email`, `avatarUrl`, `hasAvatar`, `isOfficial`, `isBlogger`,
`blogUrl`, `blog`, `locale`, `timezone`, `defaultCurrency`, `unreadMsgCount`, `counts`, `notifications`, `webSocket`,
`externalApps`, `paymentMethods`, `hasPaymentBind`, `hasPayout`, `hasSubscriptions`, `hasPaidSubscriptions`,
`hasFreeSubscriptions`, `problemSubscriptions`, `pauseSubscriptions`, `hasInfoMessages`, `dialogSettings`,
`emailConfirmed`, `hasEmailAuth`, `authSource`, `isReadOnly`, `isTotalBaned`, `isModerator`, `isAdmin`, `isTestUser`,
`isKycAllowed`, `isScheduledForDeletion`, `isTutorialFinished`, `shouldStreamRecord`, `hasAccessToDevApi`,
`residenceCluster`, `geo`, `flags`, `frontendFlags`, `featureFlags`, `privacyVersion`, `referalConvVersion`.

### `user_change_currency` — flat, handler

```ts
Flat<{ currency: "RUB" | "EUR" | "USD" | "UAH" | "BRL" | "CHF" | "SEK" | "GBP" | "AED" | "AUD" | "JPY" | "UZS" | "KZT" }>
```

### `user_ban`, `user_unban` — flat, handler

```ts
Flat<{ isTotal: boolean }>   // banned completely (true) or made read-only (false); unban lifts the same
```

### `chat_ban` — flat, handler

```ts
Flat<{
  blogId: number;          // the blog whose chat banned the user
  remainingTime: number;   // how long the ban lasts
  isByStream: boolean;     // banned from a stream's chat
}>
```

### `chat_unban` — flat, handler

```ts
Flat<{ blogId: number }>
```

### `blacklisted`, `unblacklisted` — nested, handler

```ts
Nested<{ id: number }>   // the user put on or taken off the blacklist; one handler wraps it in Number()
```

### `telegram_linked`, `max_bot_linked` — nested, handler

```ts
Nested<{ username: string }>   // the linked Telegram account or MAX bot
```

### `smile_resize`, `badge_resize`, `role_resize` — flat, handler

```ts
Flat<{
  size: string;                // which size was made; the frontend keys the result by it
  resizedId: string | number;  // id of the resized image
  url: string;
}>
```

### `blog_stat_report_ok`, `blog_stat_report_failed` — nested, REST

```ts
Nested<Report>   // the frontend stores it by type, like GET /v1/blog/stat/<blog>/csv/<type>/info → data.report

type Report = {
  type: "all" | "donations" | "paid_posts" | "paid_bundles" | "subscriptions" | "payouts" | "unsubscribe_reasons"
    | "holds" | "payments_refers" | "payments_utm" | "blog_visits_refers" | "blog_visits_utm";
  status: string;       // "completed" seen
  createdAt: number;
  url: string;          // the file
  webUrl: string;
  startDate: number | null;
  endDate: number | null;
  period: string | null;
};
```

### `blog_update` — nested, handler + REST

```ts
Nested<Partial<Blog>>   // the frontend reads coverUrl
```

`Blog` is `GET /v1/blog/<blog>`: `blogUrl`, `title`, `description`, `coverUrl`, `owner`, `count {posts, subscribers,
showcase}`, `flags`, `currency`, `acceptedCurrencies`, `paymentMethods`, `socialLinks`, `hasAdultContent`,
`subscriptionKind`, `allowedPromoTypes`, `allowedReactionList`, `reactionsDisabled`, `accessRights`, `isOwner`,
`isSubscribed`, `subscription`, `isBlackListed`, `isBlackListedByUser`, `isReadOnly`, `isTotalBaned`, `signedQuery`,
`publicWebSocketChannel`.

### `blog_verification_status` — nested, handler

```ts
{
  type: "blog_verification_status";
  data: {
    verification: {
      required?: unknown;                                  // the level of verification required
      success?: { levelName: string; review?: unknown };   // the passed verification
      last?: { levelName: string; review?: unknown };      // the latest attempt
      countryRequired?: boolean;
      countryCode?: string;
      provider?: string;
    };
  };
  extra?: { levels: Array<{ name: string }> };   // next to data, not inside it
}
```

### `info_message_create` — nested, handler

```ts
Nested<InfoMessage>   // appended to the list of GET /v1/user/info_messages → data.infoMessages

type InfoMessage = {
  id: number;
  type: string;            // e.g. kyc… for verification requests
  title: string;
  fullDescription: string;
  actionText: string;
  link: string;
  onlyPopup: boolean;      // shown as a popup instead of a banner
};
```

### `info_message_delete` — flat, handler

```ts
Flat<{ id: number }>   // the info message to remove
```

### `payment_status_advpay` — nested, handler

```ts
Nested<{
  type: "payment.succeeded" | "payment.failed" | "payment.status_updated" | "payment.on_hold" | "payment.refunded"
    | "payment_binding.created" | "payout.succeeded" | "payout.failed" | "payout_binding.created"
    | "redirect_required" | "display_qr_code";
  payment: {
    id: number;
    itemId: number | string;   // what is being paid for
    paymentStatus: { partner: string; internal: string };
  };
}>
```

### `payment_binding_created` — nested, REST

```ts
Nested<PaymentBind>   // appended to the list of GET /v1/payment/bind/list → data

type PaymentBind = {
  id: number;
  description: string;   // e.g. the masked card
  payType: string;
  payMethod: string;
  country: string;
  createAt: number;
  expireAt: number;
  deleted: boolean;
};
```

### `payout_binding_created` — nested, REST

```ts
Nested<PayoutBinding>   // like an item of GET /v1/blog/<blog>/payout/ → data

type PayoutBinding = {
  id: number;
  paySystem: string;     // the frontend lowercases it into systemId
  systemId: string;
  payType: string;
  destination: string;   // e.g. the masked card
  currency: "RUB" | "USD" | "EUR";
  isActive: boolean;
  priority: number;
  lastPayout?: { status: string; date: number };
};
```

### `bundle_access_granted` — flat, handler

```ts
Flat<{ bundleId: string | number; blogUrl: string }>   // the user got access to a bundle
```

## `dialogs#<id>`

### `dialog_message` — nested, handler + REST

```ts
Nested<DialogMessage>   // stored with the messages of GET /v1/dialog/<id>/message/

type DialogMessage = {
  id: number;
  dialogId: number;
  authorId: number;
  createdAt: number;
  data: Array<ContentBlock>;   // {type: "text", content, modificator} and media blocks (a video_file has playerUrls)
  teaser: Array<ContentBlock>;
  attachments: {
    text: { count: number };
    images: { count: number; previewUrl?: string };
    videos: { count: number; previewUrl?: string };
    audios: { count: number };
    files: { count: number };
  };
  price: number;   // for a paid message
  currencyPrices: { RUB: number; USD: number; EUR: number };
  isPaid: boolean;
  isFeePaid: boolean;
  isRead: boolean;
  isDeleted: boolean;
  payWall: boolean;
  previewType: string;
  donation?: DialogDonation;   // a donation sent with the message; the frontend shows its currencyAmounts
};

// As the donations in GET /v1/notification/standalone/event/ come
type DialogDonation = {
  id: number;
  type: "author" | "target" | "post" | "dialog";
  amount: number;            // rubles after Boosty's commission
  currencyAmounts: { RUB: number; USD: number; EUR: number };
  isFeePaid: boolean;
  createdAt: number;
  bloggerId: number;
  targetId: number;
  user: { id: number; name: string; email: string; avatarUrl: string; hasAvatar: boolean; isOfficial: boolean };
};
```

### `dialog_message_read` — nested, handler

```ts
Nested<{ lastReadMessageId: number; dialogId: number; actorId: number }>   // actorId read up to that message
```

### `dialog_message_paid` — nested, handler

```ts
Nested<{ dialogId: number; id: number }>   // a paid message was bought
```

### `dialog_delete_message` — nested, handler

```ts
Nested<{ dialogId: number; id: number }>
```

### `dialog_broadcast_messages_done` — nested, handler

```ts
Nested<{ recipientsCount: number }>   // a broadcast to subscribers finished
```

## `blogger:<owner id>`

The blog's public channel. Anyone may subscribe to any blog's one, and the web frontend never does, so what Boosty
publishes there is unknown; nothing arrived during the research.

## A dialog's `custom` channel

### `dialog_chatmate_action` — nested, handler

```ts
Nested<{ userId: number; isTyping: boolean }>   // clients publish it themselves
```

## Stream channels

| Event | Channels | Shape | Source |
|---|---|---|---|
| `message` | chat, private | `Nested<ChatMessage>`, see below | handler |
| `delete_message` | chat, private | `Flat<{ id: number }>` | handler |
| `clean_chat_messages` | chat, private | `Flat<{ userId: number }>`, all messages of that user are gone | handler |
| `stream_start` | chat, stream | nothing read; the frontend refetches the stream | handler |
| `stream_end` | chat, stream | nothing read | handler |
| `stream_like_counter` | chat, stream | `Flat<{ userId: number; counter: number }>` | handler |
| `stream_online_status` | chat, stream, global | `Flat<{ viewers: number; isOnline: boolean }>` | handler |
| `stream_info_update` | stream | listed but not handled | unknown |

The chat components read this much of a message; a stream's chat over REST (`GET /v1/blog/<blog>/video_stream/chat`)
answers 404 without a running stream, so the rest is unseen:

```ts
type ChatMessage = {
  id: number;
  author?: ChatAuthor;   // the frontend takes author, or user when there is none
  user?: ChatAuthor;
  data: Array<ContentBlock>;
  isPrivate: boolean;
  createdAt: number;
};

type ChatAuthor = {
  id: number;
  name: string;
  avatarUrl: string;
  subscription?: { levelId: number };   // the author's level in the blog, for moderators
};
```
