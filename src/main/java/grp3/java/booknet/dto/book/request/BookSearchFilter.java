package grp3.java.booknet.dto.book.request;

import grp3.java.booknet.enums.book.PublicationStatus;

public record BookSearchFilter(
        String keyword,
        String title,
        String bookCode,
        String isbn,
        String author,
        Integer categoryId,
        PublicationStatus publicationStatus,
        Boolean availableOnly,
        String sortBy,
        String sortDirection
) {
    public BookSearchFilter {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "desc";
        }
    }
}
