package grp3.java.booknet.service.admin;

import grp3.java.booknet.entity.book_service.CirculationPolicy;

public interface CirculationPolicyService {

    /** Lấy chính sách hiện hành (singleton, luôn tồn tại). */
    CirculationPolicy getPolicy();

    /** Cập nhật và lưu chính sách. Ghi lại email Admin thực hiện. */
    CirculationPolicy updatePolicy(CirculationPolicy updated, String adminEmail);
}
