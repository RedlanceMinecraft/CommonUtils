package org.redlance.common.utils.requester.mojang.namehistory.obj;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Username {
    @SerializedName(value = "name", alternate = "username")
    public String name;

    @SerializedName(value = "changedToAt", alternate = {"changedAt", "changed_at"})
    public String changedToAt;

    public OffsetDateTime getChangedToOffset(DateTimeFormatter formatter) {
        return OffsetDateTime.parse(this.changedToAt, formatter);
    }

    @Override
    public String toString() {
        return String.format("Username{name=%s, changedToAt=%s)}", name, changedToAt);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Username username) {
            return StringUtils.equalsIgnoreCase(this.name, username.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
