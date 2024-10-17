package org.redlance.common.utils.requester.mojang.obj;

import com.google.gson.annotations.SerializedName;

public class BaseMojangProfile {
    @SerializedName(value = "name", alternate = "username")
    public String name;

    @SerializedName(value = "id", alternate = "uuid")
    public String id;

    @SerializedName(value = "errorMessage", alternate = "error")
    public String errorMessage;
}
