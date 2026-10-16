package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redlance.common.requester.boosty.BoostyRequestException;
import org.redlance.common.requester.boosty.BoostyRequester;
import org.redlance.common.requester.boosty.obj.donation.Donation;
import org.redlance.common.requester.boosty.obj.post.PostSale;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.requester.boosty.obj.user.SubscriptionStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EnabledIfSystemProperty(named = "boosty.token", matches = ".*")
public class BoostyRequesterTest {
    private static final String BLOG = "dima_dencep";
    private static final String TOKEN = System.getProperty("boosty.token");
    private static final long LEVEL_ID = 2452741;

    @Test
    public void testEnum() throws IOException, InterruptedException {
        BoostyUser user = BoostyRequester.requestSubscribersChunking(List::getFirst, BLOG, TOKEN, LEVEL_ID);
        Assertions.assertNotNull(user);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, user.status());
    }

    @Test
    public void testSubscribers() throws IOException, InterruptedException {
        List<BoostyUser> users = new ArrayList<>();
        BoostyRequester.requestSubscribersChunking(page -> {
            users.addAll(page);
            return null;
        }, BLOG, TOKEN, LEVEL_ID);

        Assertions.assertFalse(users.isEmpty());
        // Messages name users by id only, so that a failure prints no personal data
        for (BoostyUser user : users) {
            Assertions.assertEquals(SubscriptionStatus.ACTIVE, user.status(), "user " + user.id());
            Assertions.assertTrue(user.isPaidSubscriber(LEVEL_ID), "user " + user.id());
            Assertions.assertNotNull(user.level(), "user " + user.id());
            Assertions.assertEquals(LEVEL_ID, user.level().id(), "user " + user.id());
        }
    }

    @Test
    public void testPostSales() throws IOException, InterruptedException {
        List<PostSale> sales = new ArrayList<>();
        BoostyRequester.requestPostSalesChunking(page -> {
            sales.addAll(page.postsSales());
            return null;
        }, BLOG, TOKEN);

        Assertions.assertFalse(sales.isEmpty());
        for (int i = 0; i < sales.size(); i++) {
            PostSale sale = sales.get(i);
            Assertions.assertTrue(sale.amount() > 0, "sale " + i);
            Assertions.assertNotNull(sale.post().id(), "sale " + i);
        }
    }

    @Test
    public void testDonations() throws IOException, InterruptedException {
        List<Donation> donations = new ArrayList<>();
        BoostyRequester.requestDonationsChunking(page -> {
            donations.addAll(page);
            return null;
        }, BLOG, TOKEN);

        Assertions.assertFalse(donations.isEmpty());
        for (int i = 0; i < donations.size(); i++) {
            Donation donation = donations.get(i);
            Assertions.assertTrue(donation.amount() > 0, "donation " + i);
            Assertions.assertNotEquals(Donation.Type.UNKNOWN, donation.type(), "donation " + i);
            Assertions.assertTrue(donation.createdAt() > 0, "donation " + i);
            Assertions.assertTrue(donation.user().id() > 0, "donation " + i);
        }

        Donation latest = donations.getFirst();
        List<Donation> alike = new ArrayList<>();
        BoostyRequester.requestDonationsChunking(page -> {
            alike.addAll(page);
            return null;
        }, BLOG, TOKEN, latest.type(), latest.targetId());

        Assertions.assertTrue(alike.contains(latest), "The filter keeps the donation it was made from");
        Assertions.assertTrue(alike.stream().allMatch(donation -> donation.type() == latest.type()
                && (latest.targetId() == 0 || donation.targetId() == latest.targetId())));
    }

    @Test
    public void testProfile() throws IOException, InterruptedException {
        BoostyUser subscriber = BoostyRequester.requestSubscribersChunking(List::getFirst, BLOG, TOKEN, LEVEL_ID);
        BoostyUser profile = BoostyRequester.requestUserProfile(BLOG, TOKEN, subscriber.id());

        Assertions.assertEquals(subscriber.id(), profile.id());
        Assertions.assertTrue(Objects.equals(subscriber.email(), profile.email()), "The e-mails differ");
        Assertions.assertTrue(subscriber.payments() == profile.payments(), "The payments differ");
        Assertions.assertNotNull(profile.level());
        Assertions.assertNotNull(profile.status());
        Assertions.assertTrue(profile.status().isActive());
        Assertions.assertFalse(profile.externalApps().isEmpty());
    }

    @Test
    public void testMissingProfile() {
        BoostyRequestException exception = Assertions.assertThrows(BoostyRequestException.class,
                () -> BoostyRequester.requestUserProfile(BLOG, TOKEN, Long.MAX_VALUE));
        Assertions.assertEquals("user_not_found", exception.error());
    }
}
