package grp3.java.booknet.controller.book;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.dto.book.request.BookCopyForm;
import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.dto.book.request.BookDeleteConfirmationForm;
import grp3.java.booknet.dto.book.response.BookDeletionCheckDto;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book.PublisherRepository;
import grp3.java.booknet.service.book.BookCopyService;
import grp3.java.booknet.service.book.BookService;
import grp3.java.booknet.service.book.BookLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/books")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BookLifecycleService bookLifecycleService;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;

    @ModelAttribute("activePage")
    public String activePage() {
        return "catalog";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("bookForm")) {
            model.addAttribute("bookForm", new BookCreateForm());
        }
        populateFormReferenceData(model);
        return "admin/books/create";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("bookForm")) {
                BookEditForm editForm = bookService.getBookForEdit(id);
                model.addAttribute("bookForm", editForm);
            }
            model.addAttribute("bookId", id);
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/catalog";
        }
    }

    @PostMapping("/{id}/edit")
    public String processEditBook(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("bookForm") BookEditForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("bookId", id);
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        }

        try {
            bookService.updateBook(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + form.getTitle() + "' has been successfully updated.");
            return "redirect:/admin/books/" + id + "/edit";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", ex.getMessage());
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        } catch (Exception ex) {
            log.error("Failed to update book ID {}: ", id, ex);
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", "An unexpected error occurred while updating the book. Please try again.");
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        }
    }

    @PostMapping("/{id}/publish")
    public String publishBook(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("bookForm") BookEditForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bookId", id);
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        }

        try {
            bookService.updateAndPublishBook(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + form.getTitle() + "' has been published.");
            return "redirect:/admin/books/" + id + "/edit";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", ex.getMessage());
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        } catch (Exception ex) {
            log.error("Failed to publish book ID {}: ", id, ex);
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", "The book could not be published. Please try again.");
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        }
    }

    @PostMapping("/create")
    public String processCreateBook(
            @Valid @ModelAttribute("bookForm") grp3.java.booknet.dto.book.request.BookCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateFormReferenceData(model);
            return "admin/books/create";
        }

        try {
            Long createdId = bookService.createBook(form);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + form.getTitle() + "' has been successfully created as DRAFT.");
            return "redirect:/admin/books/" + createdId + "/edit";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("globalError", ex.getMessage());
            populateFormReferenceData(model);
            return "admin/books/create";
        } catch (Exception ex) {
            log.error("Failed to create book: ", ex);
            model.addAttribute("globalError", "An unexpected error occurred while saving the book. Please try again.");
            populateFormReferenceData(model);
            return "admin/books/create";
        }
    }

    @GetMapping("/{id}/delete")
    public String showDeleteConfirmation(@PathVariable Long id, Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            BookDeletionCheckDto deletionCheck = bookLifecycleService.checkHardDelete(id);
            model.addAttribute("deletionCheck", deletionCheck);
            if (!model.containsAttribute("deleteForm")) {
                model.addAttribute("deleteForm", new BookDeleteConfirmationForm());
            }
            return "admin/books/delete-confirmation";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/catalog";
        }
    }

    @PostMapping("/{id}/delete")
    public String processHardDelete(@PathVariable Long id,
                                    @Valid @ModelAttribute("deleteForm") BookDeleteConfirmationForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            BookDeletionCheckDto deletionCheck = bookLifecycleService.checkHardDelete(id);
            model.addAttribute("deletionCheck", deletionCheck);
            if (bindingResult.hasErrors()) {
                model.addAttribute("bookId", id);
                model.addAttribute("bookForm", bookService.getBookForEdit(id));
                populateFormReferenceData(model);
                populateEditPageData(id, model);
                return "admin/books/edit";
            }
            bookLifecycleService.hardDelete(id, form.getConfirmationCode());
            redirectAttributes.addFlashAttribute("successMessage", "Book " + deletionCheck.getBookCode() + " was permanently deleted.");
            return "redirect:/catalog";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("globalError", ex.getMessage());
            model.addAttribute("bookId", id);
            model.addAttribute("deleteForm", form);
            if (!model.containsAttribute("bookForm")) model.addAttribute("bookForm", bookService.getBookForEdit(id));
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            return "admin/books/edit";
        } catch (Exception ex) {
            log.error("Failed to hard-delete book ID {}", id, ex);
            model.addAttribute("globalError", "The book could not be deleted safely. Please discontinue it instead.");
            model.addAttribute("bookId", id);
            model.addAttribute("deleteForm", form);
            if (!model.containsAttribute("bookForm")) model.addAttribute("bookForm", bookService.getBookForEdit(id));
            populateFormReferenceData(model);
            populateEditPageData(id, model);
            try {
                model.addAttribute("deletionCheck", bookLifecycleService.checkHardDelete(id));
                return "admin/books/edit";
            } catch (Exception lookupException) {
                redirectAttributes.addFlashAttribute("errorMessage", "The book could not be deleted safely.");
                return "redirect:/admin/books/" + id + "/edit";
            }
        }
    }

    @PostMapping("/{id}/discontinue")
    public String discontinueBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookLifecycleService.discontinue(id);
            redirectAttributes.addFlashAttribute("successMessage", "The book has been discontinued. Historical records and copy statuses were preserved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books/" + id + "/edit";
    }

    private void populateFormReferenceData(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("authors", authorRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("publishers", publisherRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    }

    private void populateEditPageData(Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        model.addAttribute("book", book);
        model.addAttribute("copies", bookCopyRepository.findByBook_Id(id));
        model.addAttribute("inventoryStats", bookCopyService.getInventoryStats(id));
        model.addAttribute("deletionCheck", bookLifecycleService.checkHardDelete(id));
        model.addAttribute("publicationStatuses", PublicationStatus.values());
        if (!model.containsAttribute("copyForm")) {
            model.addAttribute("copyForm", new BookCopyForm());
        }
        if (!model.containsAttribute("deleteForm")) {
            model.addAttribute("deleteForm", new BookDeleteConfirmationForm());
        }
    }

    @GetMapping
    public String redirectBookListToCatalog() {
        return "redirect:/catalog";
    }
}
