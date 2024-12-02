package org.redlance.common.utils.requester.spworlds.obj;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupObject {
    public String id;

    public String name;
    @Nullable
    public String description;
    public Account account;

    public List<String> followers = new ArrayList<>();
    public List<Member> members = new ArrayList<>();

    public String image;
    public String banner;

    public boolean isVerified;

    public record Member(String role, Account account) {
    }
}
