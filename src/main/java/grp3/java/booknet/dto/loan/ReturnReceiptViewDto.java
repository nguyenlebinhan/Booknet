package grp3.java.booknet.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnReceiptViewDto {
    private String receiptCode;
    private String loanCode;
    private String memberName;
    private String memberEmail;
    private String memberCode;
    private String librarianName;
    private LocalDateTime returnDate;
    private BigDecimal totalFines;
    private String notes;
    @Builder.Default
    private List<ReturnReceiptItemViewDto> items = new ArrayList<>();
}
