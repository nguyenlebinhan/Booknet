package grp3.java.booknet.service.impl.book;

import grp3.java.booknet.service.book.CoverImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LocalCoverImageStorage implements CoverImageStorage {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final Pattern STORED_FILE_NAME =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$");

    private final Path storageDirectory;

    public LocalCoverImageStorage(
            @Value("${app.storage.covers-location:uploads/covers}") String storageLocation) {
        this.storageDirectory = Path.of(storageLocation).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Cover image must not exceed 5 MB.");
        }

        byte[] content;
        String extension;
        try {
            content = image.getBytes();
            extension = detectAndValidateImage(content);
        } catch (IOException ex) {
            throw new IllegalArgumentException("The selected cover image could not be read as a valid JPEG or PNG.", ex);
        }

        String fileName = UUID.randomUUID() + extension;
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalStateException("Generated cover image path is outside the configured storage directory.");
        }

        try {
            Files.createDirectories(storageDirectory);
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return "/covers/" + fileName;
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupException) {
                ex.addSuppressed(cleanupException);
            }
            throw new UncheckedIOException("Unable to store the cover image.", ex);
        }
    }

    @Override
    public Resource load(String fileName) throws IOException {
        if (fileName == null || !STORED_FILE_NAME.matcher(fileName).matches()) {
            throw new java.nio.file.NoSuchFileException("Cover image not found.");
        }
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory) || !Files.isRegularFile(target)) {
            throw new java.nio.file.NoSuchFileException("Cover image not found.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String coverUrl) {
        if (coverUrl == null || !coverUrl.startsWith("/covers/")) {
            return;
        }

        String fileName = coverUrl.substring("/covers/".length());
        if (!STORED_FILE_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Invalid stored cover image reference.");
        }

        try {
            Files.deleteIfExists(storageDirectory.resolve(fileName).normalize());
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to delete the replaced cover image.", ex);
        }
    }

    private static String detectAndValidateImage(byte[] content) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new IllegalArgumentException("Choose a valid JPEG or PNG cover image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG and PNG cover images are supported.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw new IllegalArgumentException("Only JPEG and PNG cover images are supported.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("The cover image dimensions are too large.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Choose a valid JPEG or PNG cover image.");
                }
                decoded.flush();
                return format.equals("png") ? ".png" : ".jpg";
            } finally {
                reader.dispose();
            }
        }
    }
}
