package grp3.java.booknet.controller.librarian;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import grp3.java.booknet.dto.loan.LibrarianDeskSummaryDto;
import grp3.java.booknet.dto.loan.MemberCirculationInfoDto;
import grp3.java.booknet.dto.loan.ReturnReceiptViewDto;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import grp3.java.booknet.exception.borrowing.LoanException;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.borrowing.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping({"/staff/loans", "/librarian/loans", "/admin/loans"})
@PreAuthorize("hasAuthority('CIRCULATION')")
@RequiredArgsConstructor
public class LibrarianCirculationController {

    private final LoanService loanService;

    @ModelAttribute("activePage")
    public String activePage() {
        return "loans";
    }

    @ModelAttribute("adminName")
    public String adminName(Authentication authentication) {
        if (authentication == null) {
            return "Staff account";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails localUser) {
            return localUser.getFullName();
        }
        if (principal instanceof CustomOAuth2User googleUser) {
            return googleUser.getFullName();
        }
        return authentication.getName();
    }

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "pending_approval") String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        LibrarianDeskSummaryDto summary = loanService.getDeskSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("currentTab", tab);
        model.addAttribute("activeMenu", "loans");

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());

        switch (tab) {
            case "awaiting_handover" ->
                    model.addAttribute("awaitingHandover", loanService.getApprovedAwaitingHandover(pageRequest).getContent());
            case "active_loans" ->
                    model.addAttribute("activeLoans", loanService.getActiveBorrowings(pageRequest).getContent());
            case "pending_returns" ->
                    model.addAttribute("pendingReturns", loanService.getPendingReturns(pageRequest).getContent());
            case "history" ->
                    model.addAttribute("loanHistory", loanService.getLoanHistory(pageRequest).getContent());
            default ->
                    model.addAttribute("pendingRequests", loanService.getPendingBorrowRequests(pageRequest).getContent());
        }

        return "librarian/loans";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.approveBorrowRequest(id, getLibrarianEmail(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Borrow request approved successfully and moved to Awaiting Handover.");
        } catch (LoanException e) {
            log.warn("Cannot approve borrow request {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/loans?tab=awaiting_handover";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.rejectBorrowRequest(id, reason, getLibrarianEmail(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Borrow request rejected.");
        } catch (LoanException e) {
            log.warn("Cannot reject borrow request {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/loans?tab=pending_approval";
    }

    @PostMapping("/renew/{id}")
    public String renewLoan(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.renewLoanItem(id, getLibrarianEmail(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Loan extended successfully by 7 days.");
        } catch (LoanException e) {
            log.warn("Cannot renew loan {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/loans?tab=active_loans";
    }

    @GetMapping("/receive-return")
    public String receiveReturnDesk(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) Long requestId,
            Model model) {

        model.addAttribute("activeMenu", "record");
        model.addAttribute("keyword", keyword);
        model.addAttribute("barcode", barcode);
        model.addAttribute("selectedRequestId", requestId);

        if (keyword != null && !keyword.trim().isEmpty()) {
            MemberCirculationInfoDto patron = loanService.lookupMember(keyword.trim());
            model.addAttribute("patron", patron);
        }

        model.addAttribute("awaitingHandoverList",
                loanService.getApprovedAwaitingHandover(PageRequest.of(0, 50)).getContent());

        return "librarian/receive-return";
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam Long requestId,
            @RequestParam String copyBarcode,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            List<String> copyBarcodes = parseBarcodes(copyBarcode);
            loanService.handoverLoan(requestId, copyBarcodes, notes, getLibrarianEmail(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Physical books checked out successfully! Official 14-day loan issued.");
            return "redirect:/staff/loans?tab=active_loans";
        } catch (LoanException e) {
            log.warn("Handover failed for request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/loans/receive-return?requestId=" + requestId;
        }
    }

    private static List<String> parseBarcodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split("[,;\\r\\n\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @PostMapping("/return")
    public String processReturn(
            @RequestParam String barcode,
            @RequestParam ReturnCondition condition,
            @RequestParam(required = false) String conditionNotes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            ReturnReceiptViewDto receipt = loanService.receiveReturn(
                    barcode, condition, conditionNotes, getLibrarianEmail(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Book return processed successfully! Receipt issued.");
            return "redirect:/staff/loans/receipts/" + receipt.getReceiptCode();
        } catch (LoanException e) {
            log.warn("Return processing failed for barcode {}: {}", barcode, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/loans/receive-return?barcode=" + barcode;
        }
    }

    @GetMapping("/receipts/{code}")
    public String viewReceipt(@PathVariable String code, Model model) {
        try {
            ReturnReceiptViewDto receipt = loanService.getReturnReceipt(code);
            model.addAttribute("receipt", receipt);
            model.addAttribute("activeMenu", "loans");
            return "librarian/receipt";
        } catch (LoanException e) {
            return "redirect:/staff/loans?error=receipt_not_found";
        }
    }

    private static String getLibrarianEmail(Authentication authentication) {
        if (authentication == null) {
            return "librarian@booknet.local";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof CustomOAuth2User oauthUser) {
            return oauthUser.getEmail();
        }
        return authentication.getName();
    }
}
