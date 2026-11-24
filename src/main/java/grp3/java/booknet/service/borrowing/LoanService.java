package grp3.java.booknet.service.borrowing;

import grp3.java.booknet.dto.loan.*;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {

    LibrarianDeskSummaryDto getDeskSummary();

    Page<BorrowRequestSummaryDto> getPendingBorrowRequests(Pageable pageable);

    Page<BorrowRequestSummaryDto> getApprovedAwaitingHandover(Pageable pageable);

    Page<LoanItemDetailDto> getPendingReturns(Pageable pageable);

    Page<LoanItemDetailDto> getActiveBorrowings(Pageable pageable);

    Page<LoanItemDetailDto> getLoanHistory(Pageable pageable);

    MemberCirculationInfoDto lookupMember(String keyword);

    BorrowRequestSummaryDto getBorrowRequestDetails(Long requestId);

    void approveBorrowRequest(Long requestId, String librarianEmail);

    void rejectBorrowRequest(Long requestId, String reason, String librarianEmail);

    Loan handoverLoan(Long requestId, List<String> copyBarcodes, String notes, String librarianEmail);

    ReturnReceiptViewDto receiveReturn(String barcode, ReturnCondition condition, String conditionNotes, String librarianEmail);

    ReturnReceiptViewDto getReturnReceipt(String receiptCode);

    LoanItem renewLoanItem(Long loanItemId, String librarianEmail);
}
