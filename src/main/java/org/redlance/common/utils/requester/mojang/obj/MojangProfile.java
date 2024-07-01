package org.redlance.common.utils.requester.mojang.obj;

import org.redlance.common.utils.requester.Downloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MojangProfile {
    public long timestamp;
    public String profileId;
    public String profileName;
    public Map<String, Texture> textures;

    public Texture getSkin() {
        return this.textures.get("SKIN");
    }

    public Texture getCape() {
        return this.textures.get("CAPE");
    }

    public String getHeadTexture() {
        return "https://mc-heads.net/head/" + this.profileId;
    }

    public String getBustTexture() {
        return "https://visage.surgeplay.com/bust/512/" + this.profileId + "?no=ears"; // Fuck earsmod
    }

    public static class Texture {
        public String url;
        public Map<String, String> metadata;

        public InputStream downloadTexture() throws IOException, InterruptedException {
            return Downloader.download(this.url);
        }

        public InputStream downloadTexture(int width, int height) throws IOException, InterruptedException {
            return Downloader.downloadImage(this.url, width, height);
        }

        public boolean isSlim() {
            if (this.metadata == null) {
                return false;
            }

            return "slim".equalsIgnoreCase(this.metadata.get("model"));
        }
    }
}
