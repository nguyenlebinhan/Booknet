package grp3.java.booknet.service.impl.admin;

import grp3.java.booknet.dto.admin.AdminDashboardSummary;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummary getSummary() {
        return new AdminDashboardSummary(
                bookRepository.countByDeletedFalseAndPublicationStatus(PublicationStatus.PUBLISHED),
                userRepository.countByRole(UserRole.ROLE_USER),
                bookCopyRepository.countByStatus(CopyStatus.BORROWED),
                loanRepository.countByDueDateBeforeAndStatusIn(
                        LocalDate.now(), List.of(LoanStatus.BORROWING, LoanStatus.OVERDUE)),
                bookRepository.countByDeletedFalseAndPublicationStatus(PublicationStatus.DRAFT),
                reservationRepository.countByStatus(ReservationStatus.PENDING),
                bookCopyRepository.countByStatus(CopyStatus.AVAILABLE)
        );
    }
}
