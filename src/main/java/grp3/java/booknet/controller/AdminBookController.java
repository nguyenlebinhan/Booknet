package grp3.java.booknet.controller;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.dto.book.request.BookDeleteConfirmationForm;
import grp3.java.booknet.dto.book.response.BookDeletionCheckDto;
import grp3.java.booknet.dto.book.response.BookListRowDto;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book.PublisherRepository;
import grp3.java.booknet.service.BookService;
import grp3.java.booknet.service.BookLifecycleService;
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

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;
    private final BookLifecycleService bookLifecycleService;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;

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
            return "admin/books/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/books";
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
            return "admin/books/edit";
        }

        try {
            bookService.updateBook(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + form.getTitle() + "' has been successfully updated.");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", ex.getMessage());
            populateFormReferenceData(model);
            return "admin/books/edit";
        } catch (Exception ex) {
            log.error("Failed to update book ID {}: ", id, ex);
            model.addAttribute("bookId", id);
            model.addAttribute("globalError", "An unexpected error occurred while updating the book. Please try again.");
            populateFormReferenceData(model);
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
            return "redirect:/admin/books";
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
            return "redirect:/admin/books";
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
                return "admin/books/delete-confirmation";
            }
            bookLifecycleService.hardDelete(id, form.getConfirmationCode());
            redirectAttributes.addFlashAttribute("successMessage", "Book " + deletionCheck.getBookCode() + " was permanently deleted.");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("globalError", ex.getMessage());
            if (!model.containsAttribute("deleteForm")) model.addAttribute("deleteForm", form);
            return "admin/books/delete-confirmation";
        } catch (Exception ex) {
            log.error("Failed to hard-delete book ID {}", id, ex);
            model.addAttribute("globalError", "The book could not be deleted safely. Please discontinue it instead.");
            if (!model.containsAttribute("deleteForm")) model.addAttribute("deleteForm", form);
            try {
                model.addAttribute("deletionCheck", bookLifecycleService.checkHardDelete(id));
                return "admin/books/delete-confirmation";
            } catch (Exception lookupException) {
                redirectAttributes.addFlashAttribute("errorMessage", "The book could not be deleted.");
                return "redirect:/admin/books";
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
        return "redirect:/admin/books";
    }

    private void populateFormReferenceData(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("authors", authorRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("publishers", publisherRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    }

    @GetMapping
    public String listBooks(
            @ModelAttribute("filter") BookSearchFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        int validPage = Math.max(0, page);
        int validSize = (size <= 0 || size > 100) ? 10 : size;

        String sortBy = (filter != null && filter.sortBy() != null && !filter.sortBy().isBlank())
                ? filter.sortBy() : "id";
        Sort.Direction direction = (filter != null && "asc".equalsIgnoreCase(filter.sortDirection()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        String safeSortField = switch (sortBy) {
            case "title" -> "title";
            case "bookCode" -> "bookCode";
            case "isbn" -> "isbn";
            case "publishYear" -> "publishYear";
            default -> "id";
        };

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, safeSortField));
        Page<BookListRowDto> booksPage = bookService.getBookManagementList(filter, pageable);

        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        model.addAttribute("booksPage", booksPage);
        model.addAttribute("books", booksPage.getContent());
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", booksPage.getTotalPages());
        model.addAttribute("totalElements", booksPage.getTotalElements());
        model.addAttribute("categories", categories);
        model.addAttribute("publicationStatuses", PublicationStatus.values());
        model.addAttribute("filter", filter);

        return "admin/books/list";
    }
}
