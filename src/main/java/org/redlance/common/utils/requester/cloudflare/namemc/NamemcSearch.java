package org.redlance.common.utils.requester.cloudflare.namemc;

import org.redlance.common.utils.requester.cloudflare.namemc.generic.NameHistoryEntry;
import webGrude.mapping.annotations.Page;
import webGrude.mapping.annotations.Selector;

import java.util.ArrayList;
import java.util.List;

@Page("https://namemc.com/search?q={0}")
public class NamemcSearch {
    @Selector("body > main > div > div > div:nth-child(1) > h5 > span")
    public String username;

    public static class Profile {
        @Selector("div.card-header.py-2 > div > div.col > h3 > a")
        public String username;

        @Selector("div.card-body.px-0.py-1 > table > tbody > tr:not(.d-lg-none)")
        public List<NameHistoryEntry> nameHistory = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("Profile{name=%s, nameHistory=%s}", username, nameHistory);
        }
    }
    @Selector("body > main > div > div > div.card")
    public List<Profile> profiles;

    @Override
    public String toString() {
        return String.format("NamemcSearch{name=%s, profiles=%s}", username, profiles);
    }
}
