package org.redlance.common.utils.requester.boosty.obj.user;

import com.google.gson.annotations.SerializedName;

public enum SubscribeStatus {
    @SerializedName(value = "ACTIVE", alternate = "active")
    ACTIVE,

    @SerializedName(value = "INACTIVE", alternate = "inactive")
    INACTIVE,

    @SerializedName(value = "PAUSED", alternate = "paused")
    PAUSED,
}
