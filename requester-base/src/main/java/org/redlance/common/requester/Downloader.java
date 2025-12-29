package org.redlance.common.requester;

import com.github.mizosoft.methanol.ProgressTracker;

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

@SuppressWarnings("unused")
public class Downloader {
    public static final ProgressTracker TRACKER = ProgressTracker.newBuilder()
            .bytesTransferredThreshold(1024 * 1024) // 1024 kB
            .build();

    public static InputStream download(URI uri) throws IOException, InterruptedException {
        return Downloader.download(uri, Downloader::onProgress);
    }

    public static InputStream download(URI uri, ProgressTracker.Listener listener) throws IOException, InterruptedException {
        RequesterUtils.LOGGER.debug("Downloading {}...", uri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();

        HttpResponse<InputStream> response = Requester.HTTP_CLIENT.send(request, TRACKER.tracking(HttpResponse.BodyHandlers.ofInputStream(), listener));
        if (response.statusCode() != 200) {
            throw new IllegalStateException(String.valueOf(response.statusCode()));
        }

        return response.body();
    }

    public static InputStream downloadImage(URI uri) throws IOException, InterruptedException {
        return downloadImage(uri, 256, 256);
    }

    public static InputStream downloadImage(URI uri, int width, int height) throws IOException, InterruptedException {
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

    private static void onProgress(ProgressTracker.Progress progress) {
        if (progress.determinate()) {
            long percent = Math.round(100 * progress.value());
            RequesterUtils.LOGGER.debug("Downloaded {} from {} bytes ({})",
                    progress.totalBytesTransferred(), progress.contentLength(), percent
            );
        } else {
            RequesterUtils.LOGGER.info("Downloaded {}", progress.totalBytesTransferred());
        }

        if (progress.done()) {
            RequesterUtils.LOGGER.info("Done!");
        }
    }
}
