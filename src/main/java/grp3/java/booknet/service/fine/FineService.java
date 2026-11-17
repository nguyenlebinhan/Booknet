package grp3.java.booknet.service.fine;

import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý toàn bộ vòng đời phí phạt quá hạn.
 *
 * <pre>
 * Vòng đời Fine:
 *  (tính toán nội bộ / ESTIMATED)
 *       ↓  Librarian nhận sách vật lý → confirmReturn()
 *  UNPAID (đã chốt chính thức)
 *       ↓  Librarian xác nhận thu tiền
 *  PAID
 *  hoặc
 *       ↓  Admin miễn phí
 *  WAIVED
 * </pre>
 */
public interface FineService {

    /**
     * Tính phí ước tính cho loan đang quá hạn chưa trả.
     * <p><b>Không tạo record trong DB</b> — chỉ dùng để hiển thị cho Member (UC20).</p>
     *
     * @return Số tiền ước tính (0 nếu chưa quá hạn).
     */
    BigDecimal calculateEstimatedFine(Loan loan);

    /**
     * Chốt phí khi Librarian xác nhận đã nhận sách vật lý.
     * <p>Gọi từ {@code MemberLoanServiceImpl.confirmReturn()} sau khi lưu loan RETURNED.</p>
     * <p>Nếu không quá hạn → trả {@code Optional.empty()}, không tạo Fine.</p>
     *
     * @param loan      Loan vừa được set RETURNED (có returnedAt).
     * @param librarian User Librarian đang xác nhận.
     * @return Fine đã lưu với status=UNPAID, hoặc empty.
     */
    Optional<Fine> finalizeFine(Loan loan, User librarian);

    /**
     * Librarian thu tiền mặt tại quầy → cập nhật PAID.
     *
     * @param fineId         ID phiếu phạt.
     * @param confirmedBy    Librarian/Admin xác nhận.
     * @param transactionRef Mã giao dịch nội bộ (tuỳ chọn).
     * @return Fine đã cập nhật.
     */
    Fine confirmPaymentAtDesk(Long fineId, User confirmedBy, String transactionRef);

    /**
     * Librarian xác nhận đã nhận chuyển khoản QR → cập nhật PAID.
     *
     * @param fineId         ID phiếu phạt.
     * @param confirmedBy    Librarian/Admin xác nhận.
     * @param transactionRef Mã giao dịch ngân hàng.
     * @return Fine đã cập nhật.
     */
    Fine confirmPaymentQR(Long fineId, User confirmedBy, String transactionRef);

    /**
     * Lấy tất cả Fine của một member, mới nhất trước.
     * Dùng cho UC20 (Member xem danh sách phí của mình).
     */
    List<Fine> getMemberFines(Long memberId);

    /**
     * Admin miễn/điều chỉnh phí → status = WAIVED.
     *
     * @param fineId ID phiếu phạt.
     * @param admin  Admin thực hiện.
     * @param reason Lý do miễn phí.
     * @return Fine đã cập nhật.
     */
    Fine waiveFine(Long fineId, User admin, String reason);
}
