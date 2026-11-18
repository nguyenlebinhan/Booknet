package grp3.java.booknet.controller.catalog;

import grp3.java.booknet.dto.catalog.CategoryCreateForm;
import grp3.java.booknet.dto.catalog.CategoryOptionDto;
import grp3.java.booknet.dto.catalog.CategoryOptionsPageDto;
import grp3.java.booknet.dto.catalog.CategoryTreeItem;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.service.catalog.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final int CATEGORY_PAGE_SIZE = 20;
    private static final int CATEGORY_OPTION_PAGE_SIZE = 12;

    private final CategoryService categoryService;
    private final CategoryRepository categories;

    @GetMapping
    public String listCategories(@RequestParam(defaultValue = "0") int page, Model model) {
        int safePage = Math.max(0, page);
        Page<Category> categoriesPage = categoryService.listCategories(
                PageRequest.of(safePage, CATEGORY_PAGE_SIZE,
                        Sort.by("parent.name").ascending().and(Sort.by("name").ascending())));
        if (categoriesPage.getTotalPages() > 0 && safePage >= categoriesPage.getTotalPages()) {
            categoriesPage = categoryService.listCategories(
                    PageRequest.of(categoriesPage.getTotalPages() - 1, CATEGORY_PAGE_SIZE,
                            Sort.by("parent.name").ascending().and(Sort.by("name").ascending())));
        }
        model.addAttribute("categoriesPage", categoriesPage);
        model.addAttribute("categoryTree", buildCategoryTree(categoriesPage.getContent()));
        model.addAttribute("activePage", "categories");
        return "admin/categories/index";
    }

    @GetMapping("/options")
    @ResponseBody
    public CategoryOptionsPageDto categoryOptions(@RequestParam(defaultValue = "") String query,
                                                  @RequestParam(defaultValue = "0") int page) {
        Page<Category> categoriesPage = categoryService.searchCategories(
                query,
                PageRequest.of(Math.max(0, page), CATEGORY_OPTION_PAGE_SIZE, Sort.by("name").ascending()));
        Set<Integer> categoryIds = categoriesPage.getContent().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        Set<Integer> parentIds = categoryService.findParentIdsWithChildren(categoryIds);
        List<CategoryOptionDto> options = categoriesPage.getContent().stream()
                .map(category -> CategoryOptionDto.from(category, parentIds.contains(category.getId())))
                .toList();
        return new CategoryOptionsPageDto(
                options,
                categoriesPage.getNumber(),
                categoriesPage.getTotalPages(),
                categoriesPage.getTotalElements());
    }

    @GetMapping("/{id}/descendants")
    @ResponseBody
    public ResponseEntity<List<CategoryOptionDto>> categoryDescendants(@PathVariable Integer id) {
        if (!categories.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<CategoryOptionDto> descendants = categoryService.listDescendants(id).stream()
                .map(CategoryOptionDto::from)
                .toList();
        return ResponseEntity.ok(descendants);
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryCreateForm());
        }
        populateParentCategories(model);
        model.addAttribute("activePage", "categories");
        return "admin/categories/create";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("categoryForm") CategoryCreateForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateParentCategories(model);
            model.addAttribute("activePage", "categories");
            return "admin/categories/create";
        }

        try {
            Category created = categoryService.createCategory(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + created.getName() + "' has been created.");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "category.invalid", ex.getMessage());
            populateParentCategories(model);
            model.addAttribute("activePage", "categories");
            return "admin/categories/create";
        }
    }

    private void populateParentCategories(Model model) {
        model.addAttribute("parentCategories", categoryService.listCategories());
    }

    private List<CategoryTreeItem> buildCategoryTree(List<Category> categories) {
        Set<Integer> pageCategoryIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        Map<Integer, List<Category>> childrenByParentId = categories.stream()
                .filter(category -> category.getParent() != null)
                .filter(category -> pageCategoryIds.contains(category.getParent().getId()))
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        List<CategoryTreeItem> tree = new ArrayList<>();
        categories.stream()
                .filter(category -> category.getParent() == null)
                .forEach(category -> addCategoryTreeItems(category, 0, childrenByParentId, tree));

        Map<Integer, List<Category>> pageChildrenByExternalParent = categories.stream()
                .filter(category -> category.getParent() != null)
                .filter(category -> !pageCategoryIds.contains(category.getParent().getId()))
                .collect(Collectors.groupingBy(
                        category -> category.getParent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        pageChildrenByExternalParent.values().forEach(children -> {
            Category parent = children.get(0).getParent();
            tree.add(new CategoryTreeItem(parent.getName(), 0));
            children.forEach(child -> addCategoryTreeItems(child, 1, childrenByParentId, tree));
        });
        return tree;
    }

    private void addCategoryTreeItems(Category category,
                                      int depth,
                                      Map<Integer, List<Category>> childrenByParentId,
                                      List<CategoryTreeItem> tree) {
        tree.add(new CategoryTreeItem(category.getName(), depth));
        childrenByParentId
                .getOrDefault(category.getId(), List.of())
                .stream()
                .forEach(child -> addCategoryTreeItems(child, depth + 1, childrenByParentId, tree));
    }
}
