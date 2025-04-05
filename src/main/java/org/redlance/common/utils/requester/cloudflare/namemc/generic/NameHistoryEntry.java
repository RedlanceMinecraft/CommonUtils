package org.redlance.common.utils.requester.cloudflare.namemc.generic;

import webGrude.mapping.annotations.Selector;

import java.util.Date;

public class NameHistoryEntry {
    @Selector(value = "tr > td.text-nowrap.text-ellipsis > a")
    public String username;

    @Selector(value = "tr > td.d-none.d-lg-table-cell.text-end.text-nowrap.pe-0 > time",  format = "yyyy-MM-dd'T'HH:mm:ss.SSSX", attr = "datetime")
    public Date changedAt;

    @Override
    public String toString() {
        return String.format("NameHistoryEntry{username=%s, changedAt=%s}", username, changedAt);
    }
}
