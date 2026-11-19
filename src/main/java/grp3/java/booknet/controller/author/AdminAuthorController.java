package grp3.java.booknet.controller.author;

import grp3.java.booknet.dto.author.AuthorCreateForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.service.author.AuthorService;
import grp3.java.booknet.service.author.InvalidAuthorImageException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@Controller
@RequestMapping("/admin/authors")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuthorController {

    private static final int PAGE_SIZE = 20;
    private static final int AUTHOR_BOOK_PAGE_SIZE = 12;

    private final AuthorService authorService;

    @GetMapping
    public String listAuthors(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "") String query,
                              @RequestParam(defaultValue = "A") String letter,
                              Model model) {
        String normalizedQuery = query.trim();
        String selectedLetter = normalizeInitial(letter);
        Sort authorSort = Sort.by(Sort.Direction.ASC, "name");
        TreeSet<String> availableLetters = new TreeSet<>(authorService.listAuthorInitials());
        availableLetters.add("A");
        int safePage = Math.max(0, page);
        Page<Author> authors = authorService.listAuthorsByInitial(
                selectedLetter,
                normalizedQuery,
                PageRequest.of(safePage, PAGE_SIZE, authorSort));
        if (authors.getTotalPages() > 0 && safePage >= authors.getTotalPages()) {
            authors = authorService.listAuthorsByInitial(
                    selectedLetter,
                    normalizedQuery,
                    PageRequest.of(authors.getTotalPages() - 1, PAGE_SIZE, authorSort));
        }
        model.addAttribute("authorsPage", authors);
        model.addAttribute("letters", List.copyOf(availableLetters));
        model.addAttribute("selectedLetter", selectedLetter);
        model.addAttribute("query", normalizedQuery);
        model.addAttribute("activePage", "authors");
        return "admin/authors/index";
    }

    @GetMapping("/{id}")
    public String showAuthorBooks(@PathVariable Integer id,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "") String query,
                                  @RequestParam(defaultValue = "title-asc") String sort,
                                  Model model) {
        Author author = authorService.findAuthor(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedQuery = query.trim();
        String normalizedSort = normalizeBookSort(sort);
        Sort bookSort = bookSortFor(normalizedSort);
        int safePage = Math.max(0, page);
        Page<Book> books = authorService.searchBooksByAuthor(
                id,
                normalizedQuery,
                PageRequest.of(safePage, AUTHOR_BOOK_PAGE_SIZE, bookSort));
        if (books.getTotalPages() > 0 && safePage >= books.getTotalPages()) {
            books = authorService.searchBooksByAuthor(
                    id,
                    normalizedQuery,
                    PageRequest.of(books.getTotalPages() - 1, AUTHOR_BOOK_PAGE_SIZE, bookSort));
        }

        model.addAttribute("author", author);
        model.addAttribute("booksPage", books);
        model.addAttribute("query", normalizedQuery);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("activePage", "authors");
        return "admin/authors/details";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("authorForm")) {
            model.addAttribute("authorForm", new AuthorCreateForm());
        }
        model.addAttribute("activePage", "authors");
        return "admin/authors/create";
    }

    @PostMapping("/create")
    public String createAuthor(@Valid @ModelAttribute("authorForm") AuthorCreateForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "authors");
            return "admin/authors/create";
        }

        try {
            Author created = authorService.createAuthor(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Author '" + created.getName() + "' has been created.");
            return "redirect:/admin/authors";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "author.invalid", ex.getMessage());
            model.addAttribute("activePage", "authors");
            return "admin/authors/create";
        } catch (InvalidAuthorImageException ex) {
            bindingResult.rejectValue("image", "author.image.invalid", ex.getMessage());
            model.addAttribute("activePage", "authors");
            return "admin/authors/create";
        }
    }

    private static String normalizeBookSort(String sort) {
        return switch (sort) {
            case "title-desc", "year-desc", "recent" -> sort;
            default -> "title-asc";
        };
    }

    private static Sort bookSortFor(String sort) {
        return switch (sort) {
            case "title-desc" -> Sort.by(Sort.Order.desc("title"), Sort.Order.desc("id"));
            case "year-desc" -> Sort.by(Sort.Order.desc("publishYear"), Sort.Order.asc("title"));
            case "recent" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
        };
    }

    private static String normalizeInitial(String initial) {
        String normalized = initial == null ? "" : initial.trim();
        return normalized.isEmpty() ? "A" : normalized.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
