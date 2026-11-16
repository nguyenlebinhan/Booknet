package grp3.java.booknet.service.member;

import grp3.java.booknet.dto.member.MemberDirectoryView.*;

import java.util.List;

/** Fictional UI fixtures only. Never persisted or merged with real accounts. Snapshot: 08/11/2026. */
final class MemberPreviewData {
    private MemberPreviewData() { }

    static List<Person> people() {
        return List.of(
                person(2481, "Nguyễn Minh Anh", "minhanh.nguyen@example.com", "0903 248 161", "Active", "12/08/2026", "12/08/2027", 2, 1, "10.000đ"),
                person(2476, "Trần Hoàng Nam", "hoangnam.tran@example.com", "0912 453 678", "Active", "04/08/2026", "04/08/2027", 3, 0, "0đ"),
                person(2462, "Lê Thu Hà", "thuha.le@example.com", "0983 654 210", "Active", "20/07/2026", "20/07/2027", 1, 0, "0đ"),
                person(2435, "Phạm Đức Huy", "duchuy.pham@example.com", "0908 332 145", "Active", "14/06/2026", "14/06/2027", 1, 1, "0đ"),
                person(2318, "Võ Ngọc Linh", "ngoclinh.vo@example.com", "0935 662 790", "Expired", "31/10/2025", "31/10/2026", 0, 0, "0đ"),
                person(2297, "Đặng Gia Bảo", "giabao.dang@example.com", "0914 582 630", "Active", "01/05/2026", "01/05/2027", 2, 0, "0đ"),
                person(2489, "Bùi Khánh Vy", "khanhvy.bui@example.com", "0907 445 821", "Active", "09/01/2026", "09/01/2027", 0, 0, "0đ"),
                person(2490, "Đỗ An Nhiên", "annhien.do@example.com", "0918 620 345", "Pending", null, null, 0, 0, "0đ"));
    }

    private static Person person(long id, String name, String email, String phone, String status,
                                 String issued, String expires, long loans, long overdue, String fine) {
        return new Person(id, name, email, phone, "Hà Nội, Việt Nam", "Member", "ACTIVE", true,
                null, "TV-0" + id, status, issued, expires, loans, overdue, fine);
    }

    static List<Loan> loans(Long id) {
        // Only this sample has a detailed loan fixture; never invent loans for other samples.
        if (id != 2481L) return List.of();
        return List.of(
                new Loan("Dế Mèn phiêu lưu ký", "Tô Hoài", "VN004-03", "LM-2026-0258", "22/10/2026",
                        "05/11/2026", "3 days overdue", "Return requested 07/11 · Not received", "cover-de-men.jpg"),
                new Loan("Tôi thấy hoa vàng trên cỏ xanh", "Nguyễn Nhật Ánh", "VN001-06", "LM-2026-0279", "01/11/2026",
                        "15/11/2026", "Not yet due", "The book is with the member", "cover-hoa-vang.jpg"));
    }
}
