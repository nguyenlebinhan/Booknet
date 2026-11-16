package grp3.java.booknet.service;

import grp3.java.booknet.dto.BorrowConfirmView;
import grp3.java.booknet.dto.BorrowRequestForm;

import java.util.List;

public interface BorrowService {

    BorrowConfirmView prepareConfirmView(Long userId, List<Long> bookIds);

    void submitBorrowRequest(Long userId, BorrowRequestForm form);
}
