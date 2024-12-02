package org.redlance.common.utils.requester.mojang.obj;

import com.google.gson.annotations.SerializedName;

public class BaseMojangProfile {
    @SerializedName(value = "name", alternate = "username")
    public String name;

    @SerializedName(value = "id", alternate = {"uuid", "minecraftUUID"})
    public String id;

    @SerializedName(value = "errorMessage", alternate = "error")
    public String errorMessage;

    @Override
    public String toString() {
        return String.format("BaseMojangProfile{%s (%s)}", name, id);
    }
}
