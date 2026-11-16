package grp3.java.booknet.controller.catalog;

import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class CatalogController {

    private static final int PAGE_SIZE = 6;

    private final CatalogService catalogService;

    @GetMapping("/catalog")
    public String catalog(
            @ModelAttribute("filter") CatalogFilter filter,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        normalizeFilter(filter);
        boolean staffCatalog = isStaff(authentication);
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE, sortFor(filter.getSort()));
        Page<CatalogBookView> books = catalogService.search(filter, pageable, staffCatalog);

        int firstVisiblePage = Math.max(0, books.getNumber() - 1);
        int lastVisiblePage = Math.min(books.getTotalPages(), firstVisiblePage + 3);
        firstVisiblePage = Math.max(0, lastVisiblePage - 3);
        List<Integer> visiblePages = IntStream.range(firstVisiblePage, lastVisiblePage).boxed().toList();

        model.addAttribute("booksPage", books);
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("publishYears", catalogService.getPublishYears(staffCatalog));
        model.addAttribute("visiblePages", visiblePages);
        model.addAttribute("activePage", "catalog");
        model.addAttribute("staffCatalog", staffCatalog);
        return "catalog/index";
    }

    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_LIBRARIAN".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private static void normalizeFilter(CatalogFilter filter) {
        if (filter.getSort() == null || !List.of("recent", "title-asc", "title-desc", "year-desc").contains(filter.getSort())) {
            filter.setSort("recent");
        }
        filter.setKeyword(filter.getKeyword() == null ? "" : filter.getKeyword().trim());
    }

    private static Sort sortFor(String sort) {
        return switch (sort) {
            case "title-asc" -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
            case "title-desc" -> Sort.by(Sort.Order.desc("title"), Sort.Order.desc("id"));
            case "year-desc" -> Sort.by(Sort.Order.desc("publishYear"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }
}
