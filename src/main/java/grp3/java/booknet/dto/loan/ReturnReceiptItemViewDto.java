package grp3.java.booknet.dto.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnReceiptItemViewDto {
    private String bookTitle;
    private String copyBarcode;
    private String condition;
    private String conditionNotes;
    private BigDecimal fineAmount;
}
