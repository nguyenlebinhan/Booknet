package grp3.java.booknet.dto;

import java.time.LocalDateTime;

public record ReturnReceiptDto(
        String receiptCode,
        String loanCode,
        String memberName,
        String memberCode,
        String bookTitle,
        String barcode,
        LocalDateTime borrowedAt,
        LocalDateTime returnedAt,
        String librarianName
) {}
