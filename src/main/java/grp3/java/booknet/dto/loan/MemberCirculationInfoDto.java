package grp3.java.booknet.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCirculationInfoDto {
    private Long memberId;
    private String fullName;
    private String email;
    private String phone;
    private String memberCode;
    private String cardStatus;
    private int activeLoansCount;
    private int maxAllowedLoans;
    private BigDecimal unpaidFines;
    private boolean hasOverdue;
    private boolean eligible;
    private String ineligibilityReason;
    @Builder.Default
    private List<LoanItemDetailDto> currentBorrowings = new ArrayList<>();
}
