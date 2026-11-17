package grp3.java.booknet.controller.member;

import grp3.java.booknet.dto.member.MemberFineDto;
import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.service.fine.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller cho chức năng xem & thanh toán phí quá hạn của Member (UC20).
 */
@Slf4j
@Controller
@RequestMapping("/member/fines")
@PreAuthorize("hasAnyRole('MEMBER', 'USER')")
@RequiredArgsConstructor
public class MemberFinesController {

    private final FineService fineService;

    @GetMapping
    public String listFines(Authentication authentication, Model model) {
        Long userId = AccountPrincipal.id(authentication);
        List<MemberFineDto> fines = fineService.getMemberFineDtos(userId);

        BigDecimal totalUnpaid = fines.stream()
                .filter(MemberFineDto::isUnpaid)
                .map(MemberFineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEstimated = fines.stream()
                .filter(MemberFineDto::isEstimated)
                .map(MemberFineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = fines.stream()
                .filter(MemberFineDto::isPaid)
                .map(MemberFineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("fines", fines);
        model.addAttribute("totalUnpaid", totalUnpaid);
        model.addAttribute("totalEstimated", totalEstimated);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("activePage", "fines");

        return "member/fines/list";
    }

    @GetMapping("/{id}/pay")
    public String showPayQR(@PathVariable("id") Long fineId, Authentication authentication, Model model) {
        Long userId = AccountPrincipal.id(authentication);
        Fine fine = fineService.getFineById(fineId);

        if (!fine.getMember().getId().equals(userId)) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied: This fine does not belong to you.");
        }

        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "This fine cannot be paid right now (current status: " + fine.getStatus() + ").");
        }

        // Tạo mã tham chiếu chuyển khoản mẫu: REF-XXXXX
        String transactionRef = "REF-" + String.format("%05d", (fine.getId() * 37 + 10000) % 100000);

        model.addAttribute("fine", fine);
        model.addAttribute("transactionRef", transactionRef);
        model.addAttribute("activePage", "fines");

        return "member/fines/pay";
    }
}
