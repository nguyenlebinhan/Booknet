package grp3.java.booknet.controller.book;

import grp3.java.booknet.dto.book.request.BookCopyForm;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.BookCopyService;
import grp3.java.booknet.service.book.BookInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/books/{bookId}/copies")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminBookCopyController {

    private final BookCopyService bookCopyService;
    private final BookInventoryService bookInventoryService;
    private final BookRepository bookRepository;

    @GetMapping
    public String listCopies(@PathVariable Long bookId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Book book = findBook(bookId);
            int safePage = Math.max(0, page);
            int safeSize = size <= 0 || size > 100 ? 10 : size;
            Pageable pageable = PageRequest.of(safePage, safeSize,
                    Sort.by(Sort.Direction.DESC, "id"));
            Page<BookCopy> copies = bookCopyService.getCopies(bookId, pageable);

            model.addAttribute("book", book);
            model.addAttribute("copiesPage", copies);
            model.addAttribute("copies", copies.getContent());
            model.addAttribute("inventoryStats", bookCopyService.getInventoryStats(bookId));
            model.addAttribute("currentPage", safePage);
            model.addAttribute("totalPages", copies.getTotalPages());
            model.addAttribute("copyStatuses", CopyStatus.values());
            if (!model.containsAttribute("copyForm")) {
                model.addAttribute("copyForm", new BookCopyForm());
            }
            return "admin/books/copies/list";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/books/" + bookId + "/edit";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(@PathVariable Long bookId, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("book", findBook(bookId));
            if (!model.containsAttribute("copyForm")) {
                model.addAttribute("copyForm", new BookCopyForm());
            }
            return "admin/books/copies/create";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/catalog";
        }
    }

    @PostMapping("/create")
    public String createCopy(@PathVariable Long bookId,
                             @Valid @ModelAttribute("copyForm") BookCopyForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Check the copy details and try again.");
            return "redirect:/admin/books/" + bookId + "/edit";
        }
        try {
            BookCopy saved = bookCopyService.createCopy(bookId, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Book copy created. Barcode: " + saved.getBarcode());
            return "redirect:/admin/books/" + bookId + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/books/" + bookId + "/edit";
        } catch (Exception ex) {
            log.error("Could not create copy for book {}", bookId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not create the book copy. Please try again.");
            return "redirect:/admin/books/" + bookId + "/edit";
        }
    }

    @GetMapping("/{copyId}/edit")
    public String showEditForm(@PathVariable Long bookId, @PathVariable Long copyId,
                               Model model, RedirectAttributes redirectAttributes) {
        try {
            BookCopy copy = getCopyForBook(bookId, copyId);
            model.addAttribute("book", copy.getBook());
            model.addAttribute("copy", copy);
            if (!model.containsAttribute("copyForm")) {
                model.addAttribute("copyForm", toForm(copy));
            }
            return "admin/books/copies/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/books/" + bookId + "/copies";
        }
    }

    @PostMapping("/{copyId}/edit")
    public String updateCopy(@PathVariable Long bookId, @PathVariable Long copyId,
                             @Valid @ModelAttribute("copyForm") BookCopyForm form,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Check the copy details and try again.");
            return "redirect:/admin/books/" + bookId + "/edit";
        }
        try {
            bookCopyService.updateCopy(copyId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Book copy updated successfully.");
            return "redirect:/admin/books/" + bookId + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/books/" + bookId + "/edit";
        } catch (Exception ex) {
            log.error("Could not update copy {}", copyId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update the book copy. Please try again.");
            return "redirect:/admin/books/" + bookId + "/edit";
        }
    }

    @PostMapping("/{copyId}/status")
    public String changeStatus(@PathVariable Long bookId, @PathVariable Long copyId,
                               @RequestParam CopyStatus targetStatus,
                               RedirectAttributes redirectAttributes) {
        try {
            getCopyForBook(bookId, copyId);
            bookInventoryService.changeCopyStatus(copyId, targetStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Book copy status updated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books/" + bookId + "/copies";
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
    }

    private BookCopy getCopyForBook(Long bookId, Long copyId) {
        BookCopy copy = bookCopyService.getCopy(copyId);
        if (!copy.getBook().getId().equals(bookId)) {
            throw new IllegalArgumentException("Book copy does not belong to this book.");
        }
        return copy;
    }

    private BookCopyForm toForm(BookCopy copy) {
        BookCopyForm form = new BookCopyForm();
        form.setBarcode(copy.getBarcode());
        form.setShelfLocation(copy.getShelfLocation());
        form.setCondition(copy.getCondition());
        form.setAcquiredDate(copy.getAcquiredDate());
        return form;
    }
}
