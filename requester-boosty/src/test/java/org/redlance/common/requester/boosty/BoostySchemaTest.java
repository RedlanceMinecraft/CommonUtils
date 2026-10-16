package org.redlance.common.requester.boosty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.boosty.obj.donation.Donation;
import org.redlance.common.requester.boosty.obj.post.ContentBlock;
import org.redlance.common.requester.boosty.obj.post.PostSale;
import org.redlance.common.requester.boosty.obj.post.PostSales;
import org.redlance.common.requester.boosty.obj.user.BoostyPromo;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.requester.boosty.obj.user.Currency;
import org.redlance.common.requester.boosty.obj.user.ExternalApp;
import org.redlance.common.requester.boosty.obj.user.SubscriptionLevel;
import org.redlance.common.requester.boosty.obj.user.SubscriptionStatus;
import org.redlance.common.requester.boosty.paginators.BoostyLegacyPaginator;
import org.redlance.common.requester.boosty.paginators.BoostyPaginator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Reads responses shaped like the ones Boosty sends; the data in them is made up.
 */
class BoostySchemaTest {
    private static final TypeReference<BoostyLegacyPaginator<List<BoostyUser>>> SUBSCRIBERS = new TypeReference<>() {};
    private static final TypeReference<BoostyPaginator<PostSales>> POST_SALES = new TypeReference<>() {};
    private static final TypeReference<BoostyPaginator<BoostyRequester.Donations>> DONATIONS = new TypeReference<>() {};
    private static final TypeReference<BoostyPaginator<JsonNode>> ANY_PAGE = new TypeReference<>() {};
    private static final TypeReference<BoostyRequester.ProfileResponse> PROFILE = new TypeReference<>() {};

    @Test
    void testSubscribers() throws IOException {
        BoostyLegacyPaginator<List<BoostyUser>> page = read("subscribers.json", SUBSCRIBERS);
        Assertions.assertNull(page.error());
        Assertions.assertEquals(4, page.offset());
        Assertions.assertTrue(page.done());

        List<BoostyUser> users = page.data();
        Assertions.assertNotNull(users);
        Assertions.assertEquals(4, users.size());

        BoostyUser renewing = users.getFirst();
        Assertions.assertEquals(1001, renewing.id());
        Assertions.assertEquals("renewing@example.com", renewing.email());
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, renewing.status());
        Assertions.assertTrue(renewing.subscribed());
        Assertions.assertTrue(renewing.isFeePaid());
        Assertions.assertEquals(399, renewing.price());
        Assertions.assertEquals(521.45, renewing.payments());
        Assertions.assertEquals(1791905555L, renewing.onTime());
        Assertions.assertEquals(1794497555L, renewing.nextPayTime());
        Assertions.assertEquals(0, renewing.offTime());
        Assertions.assertNull(renewing.promo());
        Assertions.assertEquals(Map.of(), renewing.externalApps());

        SubscriptionLevel supporter = renewing.level();
        Assertions.assertNotNull(supporter);
        Assertions.assertEquals(4126895, supporter.id());
        Assertions.assertEquals("Supporter", supporter.name());
        Assertions.assertEquals(399, supporter.price());
        Assertions.assertEquals(Map.of(Currency.RUB, 399.0, Currency.USD, 5.1, Currency.EUR, 4.6),
                supporter.currencyPrices());
        Assertions.assertEquals(2121837, supporter.ownerId());
        Assertions.assertEquals(1791480659L, supporter.createdAt());
        Assertions.assertNull(supporter.parent());
        Assertions.assertEquals(Map.of(), supporter.externalApps());

        List<ContentBlock> description = supporter.data();
        Assertions.assertEquals(3, description.size());
        Assertions.assertEquals("text", description.get(0).type());
        Assertions.assertEquals("[\"Supporter perks\",\"unstyled\",[[0,0,9]]]", description.get(0).content());
        Assertions.assertNull(description.get(0).modificator());
        Assertions.assertNull(description.get(1).content());
        Assertions.assertEquals("BLOCK_END", description.get(1).modificator());
        ContentBlock image = description.get(2);
        Assertions.assertEquals("image", image.type());
        Assertions.assertEquals("6b19cd21-0c9f-47d1-b38b-c873b2972461", image.id());
        Assertions.assertEquals(480, image.width());
        Assertions.assertEquals(300, image.height());
        Assertions.assertEquals(59501, image.size());
        Assertions.assertNull(image.rendition());
        Assertions.assertFalse(image.isInvalid());

        BoostyUser trial = users.get(1);
        Assertions.assertTrue(trial.isSubscribed(), "A permanent trial renews in a century");
        Assertions.assertTrue(trial.isPaidSubscriber(), "A trial on a paid level counts");
        BoostyPromo trialLink = trial.promo();
        Assertions.assertNotNull(trialLink);
        Assertions.assertEquals(73522, trialLink.id());
        Assertions.assertEquals(BoostyPromo.Type.TRIAL_LINK, trialLink.type());
        Assertions.assertEquals(BoostyPromo.TargetType.SUBSCRIPTION_LEVEL, trialLink.targetType());
        Assertions.assertEquals("0123456789abcdef0123456789abcdef", trialLink.linkId());
        Assertions.assertEquals(1733687399L, trialLink.startTime());
        Assertions.assertEquals(0, trialLink.endTime());
        Assertions.assertTrue(trialLink.isFinished());
        Assertions.assertEquals(new BoostyPromo.Count(1, 1), trialLink.count());
        Assertions.assertEquals(new BoostyPromo.Access(true, true, false, true), trialLink.access());
        Assertions.assertEquals(new BoostyPromo.Trial(32767), trialLink.trial());
        Assertions.assertNull(trialLink.discount());

        BoostyUser ended = users.get(2);
        Assertions.assertEquals(SubscriptionStatus.INACTIVE, ended.status());
        Assertions.assertFalse(ended.subscribed());
        Assertions.assertFalse(ended.isSubscribed());
        Assertions.assertNull(ended.email(), "An empty text is read as null");
        Assertions.assertNull(ended.avatarUrl());
        Assertions.assertFalse(ended.canWrite());
        Assertions.assertTrue(ended.isBlackListed());
        Assertions.assertEquals(116.25, ended.payments());
        Assertions.assertEquals(1709677825L, ended.offTime());
        Assertions.assertEquals(1748500080L, ended.pauseFrom());
        Assertions.assertEquals(1761439995L, ended.pauseTo());

        BoostyPromo discount = ended.promo();
        Assertions.assertNotNull(discount);
        Assertions.assertEquals(BoostyPromo.Type.DISCOUNT, discount.type());
        Assertions.assertNull(discount.linkId());
        Assertions.assertEquals(1730930409L, discount.endTime());
        Assertions.assertEquals(new BoostyPromo.Count(2, 0), discount.count());
        Assertions.assertNull(discount.trial());
        Assertions.assertEquals(new BoostyPromo.Discount(25, 113, 1,
                        Map.of(Currency.RUB, 113.0, Currency.EUR, 1.28, Currency.USD, 1.43)),
                discount.discount());

        SubscriptionLevel archived = ended.level();
        Assertions.assertNotNull(archived);
        Assertions.assertTrue(archived.deleted());
        Assertions.assertTrue(archived.isArchived());
        Assertions.assertEquals(1707156059L, archived.archivedAt());
        SubscriptionLevel parent = archived.parent();
        Assertions.assertNotNull(parent);
        Assertions.assertEquals(2452741, parent.id());
        Assertions.assertEquals(150, parent.price());
        Assertions.assertEquals(Map.of(Currency.RUB, 150.0, Currency.USD, 1.9, Currency.EUR, 1.7),
                parent.currencyPrices());
        Assertions.assertEquals(List.of(), parent.data());

        BoostyUser free = users.get(3);
        Assertions.assertEquals(0, free.price());
        Assertions.assertEquals(0, free.nextPayTime());
        Assertions.assertEquals(0, free.offTime());
        Assertions.assertEquals(7582.78, free.payments());
        Assertions.assertTrue(free.isSubscribed(), "The free level runs without dates");
        Assertions.assertFalse(free.isPaidSubscriber(), "The free level is not paid");
    }

    @Test
    void testPostSales() throws IOException {
        BoostyPaginator<PostSales> page = read("post_sales.json", POST_SALES);
        Assertions.assertNull(page.error());
        Assertions.assertEquals(300, page.offset());
        Assertions.assertTrue(page.done());

        PostSales data = page.data();
        Assertions.assertNotNull(data);
        List<PostSale> sales = data.postsSales();
        Assertions.assertEquals(2, sales.size());

        PostSale sale = sales.getFirst();
        Assertions.assertEquals(529.8, sale.amount());
        Assertions.assertFalse(sale.isFeePaid());
        Assertions.assertEquals(1791544066L, sale.createdAt());
        Assertions.assertEquals(2121837L, sale.bloggerId());
        Assertions.assertEquals("dc190730-9e52-41bd-934d-61a935870045", sale.post().id());
        Assertions.assertEquals("Paid post", sale.post().title());
        Assertions.assertNull(sale.post().promo());

        BoostyUser buyer = sale.user();
        Assertions.assertEquals(1001, buyer.id());
        Assertions.assertEquals("buyer@example.com", buyer.email());
        Assertions.assertNull(buyer.avatarUrl());
        Assertions.assertTrue(buyer.canWrite());
        Assertions.assertNull(buyer.level());
        Assertions.assertNull(buyer.status());
        Assertions.assertFalse(buyer.isSubscribed(), "A buyer without a subscription");
        Assertions.assertFalse(buyer.isPaidSubscriber());

        PostSale generous = sales.get(1);
        Assertions.assertEquals(600, generous.amount());
        Assertions.assertTrue(generous.isFeePaid());
        Assertions.assertEquals(sale.post(), generous.post());
    }

    @Test
    void testDonations() throws IOException {
        BoostyPaginator<BoostyRequester.Donations> page = read("donations.json", DONATIONS);
        Assertions.assertNull(page.error());
        Assertions.assertEquals(300, page.offset());
        Assertions.assertTrue(page.done());

        BoostyRequester.Donations data = page.data();
        Assertions.assertNotNull(data);
        List<Donation> donations = data.donationsSales();
        Assertions.assertEquals(4, donations.size());

        Donation profile = donations.getFirst();
        Assertions.assertEquals(9104, profile.id());
        Assertions.assertEquals(Donation.Type.AUTHOR, profile.type());
        Assertions.assertEquals(0, profile.targetId());
        Assertions.assertEquals(471.9, profile.amount());
        Assertions.assertFalse(profile.isFeePaid());
        Assertions.assertEquals(1792072044L, profile.createdAt());
        Assertions.assertEquals(2121837L, profile.bloggerId());

        BoostyUser donor = profile.user();
        Assertions.assertEquals(1001, donor.id());
        Assertions.assertEquals("donor@example.com", donor.email());
        Assertions.assertNull(donor.avatarUrl());
        Assertions.assertTrue(donor.canWrite());
        Assertions.assertNull(donor.status());
        Assertions.assertFalse(donor.isSubscribed(), "A donor without a subscription");

        Donation goal = donations.get(1);
        Assertions.assertEquals(Donation.Type.TARGET, goal.type());
        Assertions.assertEquals(77, goal.targetId());
        Assertions.assertEquals(1000, goal.amount());
        Assertions.assertTrue(goal.isFeePaid());
        Assertions.assertFalse(goal.user().canWrite());

        Donation post = donations.get(2);
        Assertions.assertEquals(Donation.Type.POST, post.type());
        Assertions.assertEquals(5550123, post.targetId());

        Assertions.assertEquals(Donation.Type.UNKNOWN, donations.get(3).type(), "A kind Boosty may add later");
        Assertions.assertEquals(Donation.Type.DIALOG, Donation.Type.of("dialog"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> donations.add(profile));

        BoostyPaginator<BoostyRequester.Donations> empty = parse("""
                {"data": {}, "extra": {"offset": "300", "total": 0}}
                """, DONATIONS);
        Assertions.assertNotNull(empty.data());
        Assertions.assertEquals(List.of(), empty.data().donationsSales());
    }

    @Test
    void testPaging() {
        // Boosty sends the offset of post sales as a string
        BoostyPaginator<PostSales> sales = parse("""
                {"data": {"postsSales": []}, "extra": {"offset": "100", "total": 136}}
                """, POST_SALES);
        Assertions.assertEquals(100, sales.offset());
        Assertions.assertFalse(sales.done());

        BoostyPaginator<JsonNode> last = parse("""
                {"data": [], "extra": {"offset": "5", "total": 10, "isLast": true}}
                """, ANY_PAGE);
        Assertions.assertTrue(last.done());

        BoostyLegacyPaginator<List<BoostyUser>> subscribers = parse("""
                {"data": [{"id": 1}], "limit": 300, "offset": 300, "total": 310}
                """, SUBSCRIBERS);
        Assertions.assertEquals(300, subscribers.offset());
        Assertions.assertFalse(subscribers.done());
    }

    @Test
    void testErrors() {
        String json = """
                {"error": "unauthorized", "error_description": "Authorization required"}
                """;

        BoostyLegacyPaginator<List<BoostyUser>> subscribers = parse(json, SUBSCRIBERS);
        Assertions.assertEquals("unauthorized", subscribers.error());
        Assertions.assertEquals("Authorization required", subscribers.errorDescription());
        Assertions.assertNull(subscribers.data());

        BoostyPaginator<PostSales> sales = parse(json, POST_SALES);
        Assertions.assertEquals("unauthorized", sales.error());
        Assertions.assertEquals("Authorization required", sales.errorDescription());
        Assertions.assertNull(sales.data());
        Assertions.assertTrue(sales.done());

        BoostyRequestException failure = Assertions.assertInstanceOf(BoostyRequestException.class, sales.failure());
        Assertions.assertEquals("unauthorized", failure.error());
        Assertions.assertEquals("Authorization required", failure.errorDescription());
    }

    @Test
    void testEnvelopes() {
        BoostyRequester.ProfileResponse missing = parse("""
                {"error_description": "User not found", "error": "user_not_found"}
                """, PROFILE);
        Assertions.assertNull(missing.data());
        BoostyRequestException failure = missing.failure("Boosty sent no profile of user 1");
        Assertions.assertEquals("user_not_found", failure.error());
        Assertions.assertEquals("User not found", failure.errorDescription());
        Assertions.assertEquals("Boosty sent no profile of user 1: user_not_found (User not found)", failure.getMessage());
        Assertions.assertEquals("Boosty sent no page: Too many requests",
                new BoostyRequestException("Boosty sent no page", null, "Too many requests").getMessage());

        BoostyRequester.DialogResponse dialog = parse("""
                {"id": 7, "chatmate": {"id": 42, "name": "Chatmate"}, "messages": {"data": []}}
                """, BoostyRequester.DialogResponse.class);
        Assertions.assertNotNull(dialog.chatmate());
        Assertions.assertEquals(42, dialog.chatmate().id());

        BoostyRequester.SocketTokenResponse socketToken = parse("""
                {"token": "socket-token"}
                """, BoostyRequester.SocketTokenResponse.class);
        Assertions.assertEquals("socket-token", socketToken.token());
        Assertions.assertNull(socketToken.error());
    }

    @Test
    void testProfile() throws IOException {
        BoostyUser user = profile(read("profile.json", PROFILE));
        Assertions.assertEquals(1005, user.id());
        Assertions.assertEquals("Profile User", user.name());
        Assertions.assertEquals("profile@example.com", user.email());
        Assertions.assertTrue(user.isBlogger());
        Assertions.assertEquals("profileblog", user.blogUrl());
        Assertions.assertEquals(3, user.externalApps().size());
        Assertions.assertEquals(new ExternalApp("680078558781243454", "profileuser", true, false),
                user.externalApps().get("discord"));
        Assertions.assertEquals(new ExternalApp(null, null, false, false), user.externalApps().get("telegram"));

        Assertions.assertTrue(user.canWrite());
        Assertions.assertFalse(user.isBlackListed());
        Assertions.assertTrue(user.isBlackListedByUser());
        Assertions.assertEquals("Translator", user.note());
        Assertions.assertEquals(3431.1, user.payments());
        Assertions.assertEquals(132.45, user.refunds());

        Assertions.assertEquals(SubscriptionStatus.RENEWAL_CANCELED, user.status());
        Assertions.assertFalse(user.subscribed());
        Assertions.assertEquals(1728142462L, user.onTime());
        Assertions.assertEquals(1792942462L, user.offTime());
        Assertions.assertEquals(0, user.nextPayTime());
        Assertions.assertEquals(1790350462L, user.lastPayTime());

        SubscriptionLevel level = user.level();
        Assertions.assertNotNull(level);
        Assertions.assertEquals(2452741, level.id());
        Assertions.assertEquals("Beta versions", level.name());
        Assertions.assertEquals(150, level.price());
        Assertions.assertFalse(level.deleted());
        Assertions.assertEquals(List.of(), level.data());

        SubscriptionLevel.ExternalAppConfig discord = level.externalApps().get("discord");
        Assertions.assertTrue(discord.isConfigured());
        Assertions.assertNotNull(discord.data());
        Assertions.assertEquals(new SubscriptionLevel.ExternalAppConfig.Role("1183840198040232088", "Beta"),
                discord.data().role());
        Assertions.assertFalse(level.externalApps().get("telegram").isConfigured());
        Assertions.assertNull(level.externalApps().get("telegram").data());
    }

    @Test
    void testProfileWithoutSubscription() throws IOException {
        BoostyUser user = profile(read("profile_not_subscribed.json", PROFILE));
        Assertions.assertEquals(1007, user.id());
        Assertions.assertFalse(user.isBlogger());
        Assertions.assertNull(user.blogUrl());
        Assertions.assertNull(user.note());
        Assertions.assertNull(user.level());
        Assertions.assertNull(user.status());
        Assertions.assertEquals(0, user.onTime());
        Assertions.assertEquals(0, user.payments());
        Assertions.assertFalse(user.isSubscribed());
    }

    @Test
    void testProfileOfFollower() {
        BoostyUser user = profile(parse("""
                {"data": {"profile": {
                  "user": {"id": 1008, "name": "Follower"},
                  "subscription": {
                    "subscriptionLevel": {"id": 421849, "name": "Free", "price": 0},
                    "status": "active", "onTime": 1703965286, "offTime": null, "nextPayTime": null, "lastPayTime": null
                  },
                  "finance": {"payments": 0, "refunds": 0}
                }}}
                """, PROFILE));

        Assertions.assertEquals(SubscriptionStatus.ACTIVE, user.status());
        Assertions.assertFalse(user.subscribed());
        Assertions.assertTrue(user.isSubscribed(), "The free level runs without dates");
        Assertions.assertFalse(user.isPaidSubscriber(), "The free level is not paid");

        // isSubscribed() must not be written in place of the subscribed component
        JsonNode json = JacksonMappers.OBJECT_MAPPER.valueToTree(user);
        Assertions.assertTrue(json.has("subscribed"));
        Assertions.assertFalse(json.get("subscribed").booleanValue());
        Assertions.assertFalse(json.has("paidSubscriber"));
    }

    @Test
    void testProfileWithoutUser() {
        Assertions.assertNull(parse("""
                {"data": {"profile": {"id": 1009, "name": "Flat"}}}
                """, PROFILE).data().profile());
        Assertions.assertNull(parse("""
                {"data": {"profile": {"user": null, "canWrite": true}}}
                """, PROFILE).data().profile());
    }

    @Test
    void testProfileTakesOnlyKnownFields() {
        BoostyUser user = profile(parse("""
                {"data": {"profile": {
                  "user": {"id": 1010, "name": "Known"},
                  "subscription": {"id": 777, "status": "expired", "offTime": 1709677825},
                  "finance": {"id": 5, "payments": 1.5},
                  "status": "active",
                  "id": 6
                }}}
                """, PROFILE));

        Assertions.assertEquals(1010, user.id());
        Assertions.assertEquals(SubscriptionStatus.EXPIRED, user.status());
        Assertions.assertEquals(1.5, user.payments());
        Assertions.assertFalse(user.isSubscribed());
    }

    @Test
    void testIsSubscribed() {
        long now = Instant.now().getEpochSecond();
        long day = TimeUnit.DAYS.toSeconds(1);

        Assertions.assertTrue(user("nextPayTime", now + day).isSubscribed());
        Assertions.assertTrue(user("nextPayTime", now - day / 2).isSubscribed(), "A day of grace for the charge");
        Assertions.assertFalse(user("nextPayTime", now - 2 * day).isSubscribed());
        Assertions.assertTrue(user("offTime", now + day).isSubscribed());
        Assertions.assertFalse(user("offTime", now - 2 * day).isSubscribed(), "Dates outrank the reported state");
    }

    @Test
    void testIsSubscribedByState() {
        long now = Instant.now().getEpochSecond();
        long day = TimeUnit.DAYS.toSeconds(1);

        Assertions.assertFalse(user("inactive", "offTime", now - day / 2).isSubscribed(),
                "No grace once Boosty reports the end");
        Assertions.assertFalse(user("paused", "nextPayTime", now + day).isSubscribed());
        Assertions.assertFalse(user("expired", "offTime", now - day / 2).isSubscribed());
        Assertions.assertFalse(user("suspended", "nextPayTime", now + day).isSubscribed());
        Assertions.assertFalse(user("frozen", "nextPayTime", now + day).isSubscribed(),
                "A state this wrapper does not know gives no access");

        Assertions.assertTrue(user("active", "offTime", now + day).isSubscribed());
        Assertions.assertTrue(user("trial", "nextPayTime", now + day).isSubscribed());
        Assertions.assertTrue(user("renewal_canceled", "offTime", now + day).isSubscribed());
        Assertions.assertTrue(parse("{\"id\": 1, \"status\": \"active\"}", BoostyUser.class).isSubscribed(),
                "The free level runs without dates");
    }

    @Test
    void testIsPaidSubscriber() {
        Assertions.assertTrue(parse("""
                {"id": 1, "status": "active", "level": {"id": 2, "price": 150}}
                """, BoostyUser.class).isPaidSubscriber());
        Assertions.assertFalse(parse("""
                {"id": 1, "status": "active", "level": {"id": 2, "price": 0}}
                """, BoostyUser.class).isPaidSubscriber(), "The free level");
        Assertions.assertFalse(parse("""
                {"id": 1, "status": "active"}
                """, BoostyUser.class).isPaidSubscriber(), "No level");
        Assertions.assertFalse(parse("""
                {"id": 1, "status": "inactive", "level": {"id": 2, "price": 150}}
                """, BoostyUser.class).isPaidSubscriber(), "Ended");
    }

    @Test
    void testIsPaidSubscriberOfLevels() {
        BoostyUser current = parse("""
                {"id": 1, "status": "active", "level": {"id": 2452741, "price": 150}}
                """, BoostyUser.class);
        BoostyUser archived = parse("""
                {"id": 1, "status": "active", "level": {"id": 2310700, "price": 125, "isArchived": true,
                 "parent": {"id": 2452741, "name": "Beta versions", "price": 150}}}
                """, BoostyUser.class);
        BoostyUser other = parse("""
                {"id": 1, "status": "active", "level": {"id": 4126895, "price": 399}}
                """, BoostyUser.class);
        BoostyUser free = parse("""
                {"id": 1, "status": "active", "level": {"id": 421849, "price": 0}}
                """, BoostyUser.class);
        BoostyUser ended = parse("""
                {"id": 1, "status": "inactive", "level": {"id": 2452741, "price": 150}}
                """, BoostyUser.class);

        Assertions.assertTrue(current.isPaidSubscriber(2452741));
        Assertions.assertTrue(archived.isPaidSubscriber(2452741), "An archived version of the level counts");
        Assertions.assertTrue(archived.isPaidSubscriber(2310700));
        Assertions.assertFalse(other.isPaidSubscriber(2452741), "Another paid level does not");
        Assertions.assertTrue(other.isPaidSubscriber(2452741, 4126895));
        Assertions.assertTrue(other.isPaidSubscriber(new long[0]), "Every paid level when none are given");
        Assertions.assertFalse(free.isPaidSubscriber(421849), "The free level is never paid");
        Assertions.assertFalse(ended.isPaidSubscriber(2452741));
    }

    @Test
    void testUnknownValues() {
        BoostyUser user = parse("""
                {"id": 1, "status": "frozen", "promo": {"id": 1, "type": "gift", "targetType": "stream"}}
                """, BoostyUser.class);

        Assertions.assertEquals(SubscriptionStatus.UNKNOWN, user.status());
        Assertions.assertNotNull(user.promo());
        Assertions.assertEquals(BoostyPromo.Type.UNKNOWN, user.promo().type());
        Assertions.assertEquals(BoostyPromo.TargetType.UNKNOWN, user.promo().targetType());
    }

    @Test
    void testEmptyTexts() {
        BoostyUser user = parse("""
                {"id": 1, "name": "", "email": "", "avatarUrl": "", "note": "",
                 "externalApps": {"discord": {"id": "", "username": "", "hasAccount": false}}}
                """, BoostyUser.class);

        Assertions.assertNull(user.name());
        Assertions.assertNull(user.email());
        Assertions.assertNull(user.avatarUrl());
        Assertions.assertNull(user.note());
        Assertions.assertEquals(new ExternalApp(null, null, false, false), user.externalApps().get("discord"));

        BoostyUser copy = JacksonMappers.OBJECT_MAPPER.readValue(
                JacksonMappers.OBJECT_MAPPER.writeValueAsString(user), BoostyUser.class);
        Assertions.assertNull(copy.email(), "Stays null after a round trip");
        Assertions.assertEquals(user.externalApps(), copy.externalApps());
    }

    @Test
    void testUnexpectedShapes() {
        SubscriptionLevel level = parse("""
                {"id": 1, "name": "Level", "price": 150,
                 "currencyPrices": [],
                 "parent": "unexpected",
                 "externalApps": [],
                 "data": [
                   {"type": "text", "content": "[\\"Kept\\",\\"unstyled\\",[]]"},
                   {"type": "link", "content": {"url": "https://boosty.to"}},
                   {"type": "image", "width": "auto"}
                 ]}
                """, SubscriptionLevel.class);

        Assertions.assertEquals(1, level.id());
        Assertions.assertEquals(150, level.price());
        Assertions.assertEquals(Map.of(), level.currencyPrices());
        Assertions.assertNull(level.parent());
        Assertions.assertEquals(Map.of(), level.externalApps());
        Assertions.assertEquals(1, level.data().size(), "Only the blocks that do not fit are left out");
        Assertions.assertEquals("[\"Kept\",\"unstyled\",[]]", level.data().getFirst().content());

        Assertions.assertEquals(List.of(), parse("""
                {"id": 1, "data": {}}
                """, SubscriptionLevel.class).data());

        BoostyUser user = parse("""
                {"id": 1, "status": "active", "promo": [], "externalApps": [], "level": {"id": 2, "data": {}}}
                """, BoostyUser.class);
        Assertions.assertNull(user.promo());
        Assertions.assertEquals(Map.of(), user.externalApps());
        Assertions.assertNotNull(user.level());
        Assertions.assertEquals(2, user.level().id());

        PostSales sales = parse("""
                {"postsSales": [{"amount": 1, "post": {"id": "post", "title": "Post", "promo": []}, "user": {"id": 1}}]}
                """, PostSales.class);
        Assertions.assertNull(sales.postsSales().getFirst().post().promo());
    }

    @Test
    void testUnexpectedMapEntries() {
        BoostyUser user = parse("""
                {"id": 1, "externalApps": {"discord": {"id": "1", "username": "user", "hasAccount": true}, "vk": []},
                 "level": {"id": 2, "currencyPrices": {"RUB": 150, "USD": "a lot", "EUR": 1.7}}}
                """, BoostyUser.class);

        Assertions.assertEquals(Map.of("discord", new ExternalApp("1", "user", true, false)), user.externalApps(),
                "Only the entry that does not fit is left out");
        Assertions.assertNotNull(user.level());
        Assertions.assertEquals(Map.of(Currency.RUB, 150.0, Currency.EUR, 1.7), user.level().currencyPrices());
    }

    @Test
    void testCurrencies() {
        SubscriptionLevel level = parse("""
                {"id": 1, "currencyPrices": {"RUB": 150, "kzt": 900, "XYZ": 1, "ABC": 2}}
                """, SubscriptionLevel.class);

        Assertions.assertEquals(Map.of(Currency.RUB, 150.0, Currency.KZT, 900.0), level.currencyPrices(),
                "Codes in any case are read, unknown ones are left out");
        Assertions.assertEquals(Currency.USD, Currency.of("usd"));
        Assertions.assertEquals(Currency.UNKNOWN, Currency.of("XYZ"));

        JsonNode json = JacksonMappers.OBJECT_MAPPER.valueToTree(level).path("currencyPrices");
        Assertions.assertEquals(150.0, json.path("RUB").doubleValue());
        Assertions.assertEquals(900.0, json.path("KZT").doubleValue());
    }

    @Test
    void testRoundTrip() throws IOException {
        List<BoostyUser> users = new ArrayList<>(read("subscribers.json", SUBSCRIBERS).data());
        users.add(profile(read("profile.json", PROFILE)));

        for (BoostyUser user : users) {
            String json = JacksonMappers.OBJECT_MAPPER.writeValueAsString(user);
            BoostyUser copy = JacksonMappers.OBJECT_MAPPER.readValue(json, BoostyUser.class);

            Assertions.assertEquals(user, copy);
            Assertions.assertEquals(json, JacksonMappers.OBJECT_MAPPER.writeValueAsString(copy));
        }

        JsonNode json = JacksonMappers.OBJECT_MAPPER.valueToTree(users.get(2));
        Assertions.assertTrue(json.get("isBlackListed").booleanValue());
        Assertions.assertFalse(json.has("blackListed"));
    }

    @Test
    void testEquality() {
        String json = """
                {"id": 1, "name": "First", "payments": 10, "level": {"id": 2, "currencyPrices": {"RUB": 150}},
                 "promo": {"id": 3, "type": "discount", "discount": {"percent": 25, "currencyPrices": {"RUB": 113}}}}
                """;
        BoostyUser first = parse(json, BoostyUser.class);
        BoostyUser same = parse(json, BoostyUser.class);
        BoostyUser renewed = parse("""
                {"id": 1, "name": "First", "payments": 20}
                """, BoostyUser.class);

        Assertions.assertEquals(first, same);
        Assertions.assertEquals(first.hashCode(), same.hashCode());
        Assertions.assertNotEquals(first, renewed, "The same user with other data is a change");
        Assertions.assertEquals(first.id(), renewed.id());
    }

    private static BoostyUser profile(BoostyRequester.ProfileResponse response) {
        Assertions.assertNotNull(response.data());
        BoostyUser user = response.data().profile();
        Assertions.assertNotNull(user);
        return user;
    }

    private static BoostyUser user(String time, long value) {
        return parse("{\"id\": 1, \"subscribed\": true, \"" + time + "\": " + value + "}", BoostyUser.class);
    }

    private static BoostyUser user(String status, String time, long at) {
        return parse("{\"id\": 1, \"status\": \"" + status + "\", \"" + time + "\": " + at + "}", BoostyUser.class);
    }

    private static <T> T read(String name, TypeReference<T> type) throws IOException {
        try (InputStream stream = BoostySchemaTest.class.getResourceAsStream("/boosty/" + name)) {
            Assertions.assertNotNull(stream, name);
            return JacksonMappers.OBJECT_MAPPER.readValue(stream, type);
        }
    }

    private static <T> T parse(String json, TypeReference<T> type) {
        return JacksonMappers.OBJECT_MAPPER.readValue(json, type);
    }

    private static <T> T parse(String json, Class<T> type) {
        return JacksonMappers.OBJECT_MAPPER.readValue(json, type);
    }
}
