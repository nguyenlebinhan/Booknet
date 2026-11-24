package grp3.java.booknet.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibrarianDeskSummaryDto {
    private long totalPendingTasks;
    private long pendingApprovalCount;
    private long awaitingHandoverCount;
    private long pendingReturnsCount;
    private long activeBorrowingsCount;
    private long overdueCount;
}
