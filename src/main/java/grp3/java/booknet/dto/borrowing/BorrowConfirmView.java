package grp3.java.booknet.dto.borrowing;

import java.time.LocalDate;
import java.util.List;

public record BorrowConfirmView(
        List<SelectedBookItem> books,
        String memberName,
        String memberCode,
        LocalDate pickupDate,
        LocalDate returnDueDate,
        int currentActiveLoansCount,
        int maxAllowedLoans,
        int loanPeriodDays,
        int renewPeriodDays,
        boolean eligibleToBorrow,
        String ineligibleReason
) {
    public record SelectedBookItem(
            Long id,
            String title,
            String authors,
            String publisherAndYear,
            String sampleCopyBarcode,
            String shelfLocation,
            String coverUrl
    ) {}
}
