package grp3.java.booknet.controller;

import grp3.java.booknet.dto.BorrowConfirmView;
import grp3.java.booknet.dto.BorrowRequestForm;
import grp3.java.booknet.exception.AccountException;
import grp3.java.booknet.security.AccountPrincipal;
import grp3.java.booknet.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/loans")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @GetMapping("/confirm")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String showConfirmLoanPage(
            Authentication authentication,
            @RequestParam(value = "bookId", required = false) List<Long> bookIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);

        if (bookIds == null || bookIds.isEmpty()) {
            model.addAttribute("view", null);
            model.addAttribute("borrowForm", new BorrowRequestForm());
            return "borrow-confirm";
        }

        try {
            BorrowConfirmView confirmView = borrowService.prepareConfirmView(userId, bookIds);
            model.addAttribute("view", confirmView);

            BorrowRequestForm form = BorrowRequestForm.builder()
                    .bookIds(new ArrayList<>(bookIds))
                    .termsAccepted(false)
                    .build();
            model.addAttribute("borrowForm", form);

            return "borrow-confirm";
        } catch (AccountException e) {
            log.warn("Cannot prepare loan confirm view: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/confirm/remove")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String removeBookFromLoanRequest(
            @RequestParam(value = "bookIds", required = false) List<Long> bookIds,
            @RequestParam("removeBookId") Long removeBookId,
            RedirectAttributes redirectAttributes) {

        if (bookIds == null || bookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No books in loan request.");
            return "redirect:/";
        }

        List<Long> remainingBookIds = bookIds.stream()
                .filter(id -> !id.equals(removeBookId))
                .toList();

        if (remainingBookIds.isEmpty()) {
            return "redirect:/loans/confirm";
        }

        String queryParams = remainingBookIds.stream()
                .map(id -> "bookId=" + id)
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        return "redirect:/loans/confirm?" + queryParams;
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String processConfirmLoan(
            Authentication authentication,
            @Valid @ModelAttribute("borrowForm") BorrowRequestForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);

        if (bindingResult.hasErrors()) {
            try {
                BorrowConfirmView confirmView = borrowService.prepareConfirmView(userId, form.getBookIds());
                model.addAttribute("view", confirmView);
                return "borrow-confirm";
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Session expired or invalid book list. Please try again.");
                return "redirect:/";
            }
        }

        try {
            borrowService.submitBorrowRequest(userId, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your loan request has been sent to the librarians. Please visit the circulation desk to collect your book(s).");
            return "redirect:/?loan_requested=true";
        } catch (AccountException e) {
            log.warn("Borrow request rejected: {}", e.getMessage());
            try {
                BorrowConfirmView confirmView = borrowService.prepareConfirmView(userId, form.getBookIds());
                model.addAttribute("view", confirmView);
                model.addAttribute("errorMessage", e.getMessage());
                return "borrow-confirm";
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/";
            }
        } catch (Exception e) {
            log.error("Error submitting borrow request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to submit your loan request. Please try again later.");
            return "redirect:/";
        }
    }
}
