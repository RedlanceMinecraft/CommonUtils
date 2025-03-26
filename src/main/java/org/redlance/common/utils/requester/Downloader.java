package org.redlance.common.utils.requester;

import org.redlance.common.CommonUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Downloader {
    public static InputStream download(String uri) throws IOException, InterruptedException {
        CommonUtils.LOGGER.debug("Downloading {}...", uri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

        HttpResponse<InputStream> response = Requester.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(String.valueOf(response.statusCode()));
        }

        return response.body();
    }

    public static InputStream downloadImage(String uri) throws IOException, InterruptedException {
        return downloadImage(uri, 256, 256);
    }

    public static InputStream downloadImage(String uri, int width, int height) throws IOException, InterruptedException {
        try (InputStream body = download(uri)) {
            byte[] resized = resizeImage(body, width, height);

            try (InputStream out = new ByteArrayInputStream(resized)) {
                return out;
            }
        }
    }

    public static byte[] resizeImage(InputStream body, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(body);

        width = Math.min(width, originalImage.getWidth());
        height = Math.min(height, originalImage.getHeight());

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        graphics2D.dispose();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(resizedImage, "png", os);
            return os.toByteArray();
        }
    }
}
