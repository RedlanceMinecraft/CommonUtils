/*package org.redlance.common.utils.requester.mojang.namehistory;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.utils.requester.mojang.namehistory.obj.Username;

import java.io.IOException;
import java.util.List;

public interface INameProvider {
    List<Username> getNameHistoryByName(String name) throws IOException, InterruptedException;
    List<Username> getNameHistoryById(String uuid) throws IOException, InterruptedException;

    record Response(@SerializedName(value = "usernames", alternate = {"name_history", "names"}) List<Username> usernames) {
        public static Response parse(JsonObject object) {
            return Serializer.getSerializer().fromJson(object, Response.class);
        }
    }
}*/
