package grp3.java.booknet.dto.loan;

import grp3.java.booknet.enums.book_service.BorrowRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequestSummaryDto {
    private Long requestId;
    private String requestCode;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
    private String memberCode;
    private LocalDateTime requestDate;
    private BorrowRequestStatus status;
    @Builder.Default
    private List<String> bookTitles = new ArrayList<>();
    @Builder.Default
    private List<Long> bookIds = new ArrayList<>();
    private int bookCount;
    private String rejectionReason;
}
