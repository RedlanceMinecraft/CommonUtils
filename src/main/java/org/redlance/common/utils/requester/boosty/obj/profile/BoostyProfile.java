package org.redlance.common.utils.requester.boosty.obj.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.redlance.common.utils.requester.boosty.obj.user.BoostyUser;

public class BoostyProfile {
    /**
     * If you can write to this user.
     */
    public boolean canWrite;

    /**
     * Idk.
     */
    public FinanceObj finance;

    /**
     * true if the user is on **your** blacklist.
     */
    public boolean isBlackListed;

    /**
     * true if you are blacklisted by a user.
     */
    public boolean isBlackListedByUser;

    /**
     * A note about the user.
     */
    public String note;

    /**
     * User subscription level.
     */
    @JsonAlias(value = {"level", "subscription"})
    public Subscription level;

    /**
     * I don't know why, but there are unique fields here too.
     */
    public BoostyUser user;

    @Override
    public String toString() {
        return String.format("BoostyProfile{%s (%s)}", user, level);
    }
}
