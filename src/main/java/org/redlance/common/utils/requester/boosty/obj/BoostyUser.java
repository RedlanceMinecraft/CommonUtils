package org.redlance.common.utils.requester.boosty.obj;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class BoostyUser {
    public boolean canWrite;
    public String avatarUrl;
    public boolean hasAvatar;
    public String email;
    public int id;
    public String name;
    public boolean isBlackListed;
    public long nextPayTime;
    public long onTime;
    public long offTime;
    public boolean subscribed;
    public Map<String, ExternalApp> externalApps;

    @Override
    public String toString() {
        return String.format("BoostyUser{%s - %s (%s)}", name, email, id);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BoostyUser boostyUser) {
            if (this.id == 0)
                return false; // Custom

            return this.id == boostyUser.id;
        }

        if (other instanceof String string) {
            return Objects.equals(email, string) || Objects.equals(name, string);
        }

        if (other instanceof Number in) {
            return this.id == in.intValue();
        }

        return false;
    }

    public boolean isSubscribed() {
        if (onTime > 0) {
            long seconds = Instant.now(Clock.systemUTC()).getEpochSecond() - 86400;

            if (nextPayTime > 0) { // subscribed
                return nextPayTime > seconds;
            }

            if (offTime > 0) { // unsubscribed
                return offTime > seconds;
            }

            return false;
        }

        return true;
    }

    public static class ExternalApp {
        public boolean hasAccount;
        public String username;

        @Override
        public String toString() {
            return "ExternalApp{" + this.username + "}";
        }
    }
}
