/*package org.redlance.common.utils.requester.spworlds.obj;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupObject {
    public String id;

    public String name;
    @Nullable
    public String description;
    public Account account;

    public List<Follower> followers = new ArrayList<>();
    public List<Member> members = new ArrayList<>();

    public String image;
    public String banner;

    public boolean isVerified;

    public record Member(String role, Account account) {
    }

    public record Follower(String id) {
    }

    @Override
    public String toString() {
        return String.format("{id=%s, name=%s, description=%s, owner=%s}", this.id, this.name, this.description, this.account);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GroupObject object && this.id.equals(object.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}*/
