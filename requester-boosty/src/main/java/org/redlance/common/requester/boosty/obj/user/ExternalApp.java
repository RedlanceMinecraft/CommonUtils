package org.redlance.common.requester.boosty.obj.user;

public class ExternalApp {
    /**
     * true if the account is linked.
     */
    public boolean hasAccount;

    /**
     * User's nickname in the social network, for discord the tag is used.
     */
    public String username;

    @Override
    public String toString() {
        return "ExternalApp{" + this.username + "}";
    }
}
