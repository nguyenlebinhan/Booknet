package grp3.java.booknet.enums.book_service;

public enum FineStatus {
    /**
     * Phí ước tính — sách chưa được Librarian nhận vật lý.
     * Chỉ hiển thị, không cho phép thanh toán, không block mượn thêm.
     */
    ESTIMATED,

    /**
     * Librarian đã nhận sách, phí đã chốt chính thức — chưa thanh toán.
     * Trước đây map 1-1 với UNPAID; giữ UNPAID để BorrowServiceImpl check block.
     */
    UNPAID,

    /** Đã thanh toán (tại quầy hoặc QR). */
    PAID,

    /** Admin miễn phí — vẫn giữ record. */
    WAIVED
}
