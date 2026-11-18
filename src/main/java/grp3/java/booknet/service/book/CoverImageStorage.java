package grp3.java.booknet.service.book;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CoverImageStorage {

    String store(MultipartFile image);

    Resource load(String fileName) throws IOException;

    void delete(String coverUrl);
}
