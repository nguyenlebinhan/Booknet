package grp3.java.booknet.dto.book.response;

import lombok.Builder;

@Builder
public record InventoryStatsDto(
        Long bookId,
        String bookTitle,
        String bookCode,
        long totalCopies,
        long availableCopies,
        long borrowedCopies,
        long reservedCopies,
        long damagedCopies,
        long discontinuedCopies,
        long lostCopies
) {}
