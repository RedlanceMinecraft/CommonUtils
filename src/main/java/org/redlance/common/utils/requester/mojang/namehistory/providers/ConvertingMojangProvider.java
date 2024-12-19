package org.redlance.common.utils.requester.mojang.namehistory.providers;

import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.namehistory.obj.Username;
import org.redlance.common.utils.requester.mojang.obj.MojangProfile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Like {@link MojangApiProvider}, but it will additionally
 * try to convert the uuid if nothing is found for the original one
 */
public class ConvertingMojangProvider extends MojangApiProvider {
    public ConvertingMojangProvider(String endpoint) {
        super(endpoint);
    }

    @Override
    public List<Username> getNameHistoryById(String uuid, boolean converted) throws IOException, InterruptedException {
        if (converted) {
            return super.getNameHistoryById(uuid, true);
        }

        List<Username> usernames = getNameHistoryByIdSafe(uuid);

        if (usernames.isEmpty()) { // Most likely the uuid is official, convert it to elyby
            Optional<String> profileName = MojangRequester.getMojangProfileById(uuid).map(MojangProfile::getProfileName);
            if (profileName.isEmpty()) {
                return Collections.emptyList();
            }

            return super.getNameHistoryById(MojangRequester.getIdByName(this.endpoint, profileName.get()), true);
        }

        return usernames;
    }

    protected List<Username> getNameHistoryByIdSafe(String uuid) {
        try {
            return super.getNameHistoryById(uuid, false);
        } catch (Throwable th) {
            return Collections.emptyList();
        }
    }
}
