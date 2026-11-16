package grp3.java.booknet.dto.catalog;

import grp3.java.booknet.enums.book.PublicationStatus;

import java.util.List;

public record CatalogBookView(
        Long id,
        String title,
        List<String> authors,
        List<String> categories,
        String coverUrl,
        String language,
        Short publishYear,
        long totalCopies,
        long availableCopies,
        boolean canReserve,
        PublicationStatus publicationStatus
) {
}
