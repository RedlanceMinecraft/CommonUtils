package org.redlance.common.utils.requester.cloudflare.namemc;

import org.redlance.common.utils.requester.cloudflare.namemc.generic.NameHistoryEntry;
import webGrude.mapping.annotations.Page;
import webGrude.mapping.annotations.Selector;

import java.util.ArrayList;
import java.util.List;

@Page("https://namemc.com/profile/{0}.{1}")
public class NamemcProfile {
    @Selector("body > main > div.row.align-items-end > div > h1")
    public String username;

    @Selector("body > main > div:nth-child(3) > div.col-md.order-md-2.profile-column-right > div:nth-child(1) > div.card-body.py-1")
    public static class ProfileInfo {
        @Selector("div:nth-child(1) > div.col-12.order-lg-2.col-lg > select > option:nth-child(1)")
        public String uuid;

        @Selector(value = "div:nth-child(2) > div.col-auto", format = "([0-9,]*) / month", defValue = "0")
        public String views;

        public static class Connection {
            @Selector(value = "a", attr = "data-bs-content")
            public String name;

            @Selector(value = "a", attr = "href")
            public String href;

            @Selector(value = "a > img", attr = "src")
            public String icon;

            @Override
            public String toString() {
                return String.format("Connection{name=%s, href=%s}", name, href);
            }
        }

        @Selector(value = "div:nth-child(4) > div.col.d-flex.flex-wrap.justify-content-end.justify-content-lg-start > a")
        public List<Connection> connections = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("ProfileInfo{uuid=%s, views=%s, connections=%s}", uuid, views, connections);
        }
    }
    public ProfileInfo info;

    @Selector(value = "body > main > div:nth-child(3) > div.col-md.order-md-2.profile-column-right > div:nth-child(3) > div.card-body.px-0.py-1 > table > tbody > tr:not(.d-lg-none)")
    public List<NameHistoryEntry> nameHistory = new ArrayList<>();

    @Override
    public String toString() {
        return String.format("NamemcProfile{name=%s, info=%s, nameHistory=%s}", username, info, nameHistory);
    }
}
