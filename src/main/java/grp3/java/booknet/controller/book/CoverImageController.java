package grp3.java.booknet.controller.book;

import grp3.java.booknet.service.book.CoverImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class CoverImageController {

    private final CoverImageStorage coverImageStorage;

    @GetMapping("/covers/{fileName}")
    public ResponseEntity<Resource> coverImage(@PathVariable String fileName) {
        try {
            Resource image = coverImageStorage.load(fileName);
            MediaType contentType = fileName.toLowerCase(Locale.ROOT).endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(image);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cover image not found.", ex);
        }
    }
}
