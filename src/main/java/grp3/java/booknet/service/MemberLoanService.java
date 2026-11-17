package grp3.java.booknet.service;

import grp3.java.booknet.dto.MemberLoansView;
import grp3.java.booknet.dto.ReturnDetailView;
import grp3.java.booknet.dto.ReturnReceiptDto;

public interface MemberLoanService {

    MemberLoansView getMemberLoansView(Long userId);

    void requestReturnBook(Long userId, Long loanId);

    void requestRenewLoan(Long userId, Long loanId);

    ReturnReceiptDto getReturnReceipt(Long userId, Long loanId);

    /** Trang Returns & receipts — member (chủ phiếu) hoặc librarian đều xem được. */
    ReturnDetailView getReturnDetail(Long requesterId, Long loanId, boolean isLibrarian);

    /** Librarian xác nhận đã nhận sách tại quầy, cập nhật status → RETURNED. */
    void confirmReturn(Long librarianId, Long loanId, String conditionNote);
}
