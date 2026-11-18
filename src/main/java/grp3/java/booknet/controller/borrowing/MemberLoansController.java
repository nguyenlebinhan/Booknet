package grp3.java.booknet.controller.borrowing;

import grp3.java.booknet.dto.borrowing.MemberLoansView;
import grp3.java.booknet.dto.borrowing.ReturnDetailView;
import grp3.java.booknet.dto.borrowing.ReturnReceiptDto;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.service.borrowing.MemberLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberLoansController {

    private final MemberLoanService memberLoanService;

    @GetMapping("/my-books")
    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'ADMIN')")
    public String showMyBooks(
            Authentication authentication,
            @RequestParam(value = "tab", defaultValue = "current") String tab,
            @RequestParam(value = "receiptId", required = false) Long receiptId,
            Model model) {

        Long userId = AccountPrincipal.id(authentication);
        MemberLoansView loansView = memberLoanService.getMemberLoansView(userId);
        model.addAttribute("view", loansView);
        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "my-books");

        if (receiptId != null) {
            try {
                ReturnReceiptDto receipt = memberLoanService.getReturnReceipt(userId, receiptId);
                model.addAttribute("receipt", receipt);
            } catch (AccountException e) {
                log.warn("Cannot fetch return receipt: {}", e.getMessage());
            }
        }

        return "my-books";
    }

    @PostMapping("/loans/{id}/return")
    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'ADMIN')")
    public String requestReturn(
            Authentication authentication,
            @PathVariable("id") Long loanId,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);
        try {
            memberLoanService.requestReturnBook(userId, loanId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Return request submitted! Please bring the book to the circulation desk for the librarian to accept.");
        } catch (AccountException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error requesting book return for loan {}", loanId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to submit return request right now.");
        }
        return "redirect:/member/my-books";
    }

    @PostMapping("/loans/{id}/renew")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('RENEW')")
    public String requestRenew(
            Authentication authentication,
            @PathVariable("id") Long loanId,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);
        try {
            memberLoanService.requestRenewLoan(userId, loanId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Renewal request submitted for 7 more days. Awaiting librarian confirmation.");
        } catch (AccountException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error requesting renewal for loan {}", loanId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to submit renewal request right now.");
        }
        return "redirect:/member/my-books";
    }

    // ── Returns & receipts ──────────────────────────────────────────────────

    /**
     * Trang Returns & receipts — member (chủ phiếu) + librarian đều xem được.
     */
    @GetMapping("/loans/{id}/return-detail")
    @PreAuthorize("hasRole('MEMBER') or hasAuthority('MEMBER_READ') or hasAuthority('CIRCULATION')")
    public String showReturnDetail(
            Authentication authentication,
            @PathVariable("id") Long loanId,
            Model model) {

        Long requesterId = AccountPrincipal.id(authentication);
        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MEMBER_READ") || a.getAuthority().equals("CIRCULATION"));

        try {
            ReturnDetailView detail = memberLoanService.getReturnDetail(requesterId, loanId, isLibrarian);
            model.addAttribute("detail", detail);
            model.addAttribute("isLibrarian", isLibrarian);
            model.addAttribute("activePage", "my-books");
            return "return-detail";
        } catch (AccountException e) {
            log.warn("Cannot load return detail for loan {}: {}", loanId, e.getMessage());
            return "redirect:/member/my-books";
        }
    }

    /**
     * Librarian xác nhận đã nhận sách tại quầy → status RETURNED.
     */
    @PostMapping("/loans/{id}/return/confirm")
    @PreAuthorize("hasAuthority('CIRCULATION')")
    public String confirmReturn(
            Authentication authentication,
            @PathVariable("id") Long loanId,
            @RequestParam(value = "conditionNote", required = false) String conditionNote,
            RedirectAttributes redirectAttributes) {

        Long librarianId = AccountPrincipal.id(authentication);
        try {
            memberLoanService.confirmReturn(librarianId, loanId, conditionNote);
            redirectAttributes.addFlashAttribute("successMessage", "Return confirmed successfully.");
        } catch (AccountException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error confirming return for loan {}", loanId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to confirm return. Please try again.");
        }
        return "redirect:/member/loans/" + loanId + "/return-detail";
    }
}
