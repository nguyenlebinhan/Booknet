package grp3.java.booknet.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanItemDetailDto {
    private Long loanItemId;
    private Long loanId;
    private String loanCode;
    private String memberName;
    private String memberEmail;
    private String copyBarcode;
    private String bookTitle;
    private LocalDate dueDate;
    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;
    private int renewCount;
    private String status;
    private boolean overdue;
    private long daysOverdue;
    private BigDecimal fineAmount;
    private String returnCondition;
    private String damageNotes;
}
