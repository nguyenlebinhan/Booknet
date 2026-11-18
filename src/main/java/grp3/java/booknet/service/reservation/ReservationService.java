package grp3.java.booknet.service.reservation;

import grp3.java.booknet.dto.reservation.LibrarianReservationsView;
import grp3.java.booknet.dto.reservation.MemberReservationsView;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;

public interface ReservationService {

    MemberReservationsView getMemberReservationsView(Long memberId, Long cancelId);

    Reservation reserveBook(Long memberId, Long bookId);

    void cancelMemberReservation(Long memberId, Long reservationId);

    LibrarianReservationsView getLibrarianReservationsView(String tab, String keyword);

    Reservation assignCopyAndMarkReady(Long reservationId, String librarianEmail);

    Loan fulfillReservation(Long reservationId, String librarianEmail);

    void cancelReservationByStaff(Long reservationId, String reason, String librarianEmail);
}
