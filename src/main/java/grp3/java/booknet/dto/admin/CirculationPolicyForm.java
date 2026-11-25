package grp3.java.booknet.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Form DTO để cập nhật chính sách lưu thông thư viện (UC44).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirculationPolicyForm {

    @Min(value = 1, message = "Borrowing quota must be at least 1 book.")
    private int maxLoansPerMember;

    @Min(value = 1, message = "Standard loan period must be at least 1 day.")
    private int loanPeriodDays;

    @Min(value = 0, message = "Renewal period cannot be negative.")
    private int renewPeriodDays;

    @NotNull(message = "Daily fine rate is required.")
    @DecimalMin(value = "0.0", message = "Daily fine rate must be non-negative.")
    private BigDecimal dailyFineRate;

    @Min(value = 1, message = "Reservation hold period must be at least 1 day.")
    private int reservationHoldDays;
}
