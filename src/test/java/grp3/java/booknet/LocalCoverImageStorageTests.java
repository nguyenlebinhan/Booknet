package grp3.java.booknet;

import grp3.java.booknet.service.impl.book.LocalCoverImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalCoverImageStorageTests {

    @TempDir
    Path storageDirectory;

    @Test
    void storesImageUsingGeneratedNameAndCanLoadAndDeleteIt() throws IOException {
        LocalCoverImageStorage storage = new LocalCoverImageStorage(storageDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
                "coverImage", "cover.png", "image/png", validPng());

        String coverUrl = storage.store(image);
        String fileName = coverUrl.substring("/covers/".length());

        assertThat(coverUrl).matches("/covers/[0-9a-f-]{36}\\.png");
        assertThat(storage.load(fileName).getInputStream().readAllBytes()).isNotEmpty();
        assertThat(Files.exists(storageDirectory.resolve(fileName))).isTrue();

        storage.delete(coverUrl);

        assertThat(Files.exists(storageDirectory.resolve(fileName))).isFalse();
    }

    @Test
    void rejectsContentThatIsNotAnImage() {
        LocalCoverImageStorage storage = new LocalCoverImageStorage(storageDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
                "coverImage", "cover.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG and PNG");
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() {
        LocalCoverImageStorage storage = new LocalCoverImageStorage(storageDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
                "coverImage", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void rejectsPathTraversalWhenLoadingStoredFiles() {
        LocalCoverImageStorage storage = new LocalCoverImageStorage(storageDirectory.toString());

        assertThatThrownBy(() -> storage.load("../cover.png"))
                .isInstanceOf(java.nio.file.NoSuchFileException.class);
    }

    private static byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
