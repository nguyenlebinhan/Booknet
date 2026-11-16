package grp3.java.booknet.service.borrowing;

import grp3.java.booknet.dto.borrowing.BorrowConfirmView;
import grp3.java.booknet.dto.borrowing.BorrowRequestForm;

import java.util.List;

public interface BorrowService {

    BorrowConfirmView prepareConfirmView(Long userId, List<Long> bookIds);

    void submitBorrowRequest(Long userId, BorrowRequestForm form);
}
