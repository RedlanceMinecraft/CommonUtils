package org.redlance.common.utils.requester.spworlds.obj;

import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;

public class Account {
    public String id;
    public boolean isBanned;
    public BaseMojangProfile user;

    @Override
    public String toString() {
        return String.format("Account{id=%s, isBanned=%s, user=%s}", this.id, this.isBanned, this.user);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Account account && this.id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
