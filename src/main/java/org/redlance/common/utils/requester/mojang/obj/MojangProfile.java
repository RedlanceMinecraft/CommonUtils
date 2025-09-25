package org.redlance.common.utils.requester.mojang.obj;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.utils.requester.Downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@SuppressWarnings("unused")
public record MojangProfile(long timestamp, String profileId, String profileName, Map<String, Texture> textures) {
    public @Nullable Texture getSkin() {
        return this.textures.get("SKIN");
    }

    public @Nullable Texture getCape() {
        return this.textures.get("CAPE");
    }

    public String getHeadTexture() {
        return "https://mc-heads.net/head/" + this.profileId;
    }

    public String getBustTexture() {
        return "https://visage.surgeplay.com/bust/512/" + this.profileId + "?no=ears"; // Fuck earsmod
    }

    public record Texture(String url, Map<String, String> metadata) {
        public InputStream downloadTexture() throws IOException, InterruptedException {
            return Downloader.download(URI.create(this.url));
        }

        public InputStream downloadTexture(int width, int height) throws IOException, InterruptedException {
            return Downloader.downloadImage(URI.create(this.url), width, height);
        }

        public boolean isSlim() {
            if (this.metadata == null) return false;
            return "slim".equalsIgnoreCase(this.metadata.get("model"));
        }
    }

    @Override
    public @NotNull String toString() {
        return String.format("MojangProfile{profileName=%s, profileId=%s}", profileName(), profileId());
    }
}
