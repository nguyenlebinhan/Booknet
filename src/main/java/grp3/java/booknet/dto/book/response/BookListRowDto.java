package grp3.java.booknet.dto.book.response;

import grp3.java.booknet.enums.book.PublicationStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record BookListRowDto(
        Long id,
        String bookCode,
        String isbn,
        String title,
        List<String> authors,
        List<String> categories,
        String publisherName,
        Short publishYear,
        String language,
        PublicationStatus publicationStatus,
        boolean deleted,
        long totalCopies,
        long availableCopies
) {}
