package grp3.java.booknet.service.impl.catalog;

import grp3.java.booknet.dto.catalog.CategoryCreateForm;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.service.catalog.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categories;

    @Override
    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categories.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> listCategories(Pageable pageable) {
        return categories.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> searchCategories(String name, Pageable pageable) {
        String searchTerm = name == null ? "" : name.trim();
        return searchTerm.isEmpty()
                ? categories.findAll(pageable)
                : categories.findByNameContainingIgnoreCase(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listDescendants(Integer categoryId) {
        List<Category> descendants = new ArrayList<>();
        List<Integer> parentIds = List.of(categoryId);
        while (!parentIds.isEmpty()) {
            List<Category> children = categories.findAllByParent_IdInOrderByNameAsc(parentIds);
            descendants.addAll(children);
            parentIds = children.stream().map(Category::getId).toList();
        }
        return descendants;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> findParentIdsWithChildren(Collection<Integer> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Set.of();
        }
        return categories.findParentIdsWithChildren(categoryIds);
    }

    @Override
    @Transactional
    public Category createCategory(CategoryCreateForm form) {
        String name = form.getName().trim();
        if (categories.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }

        Category parent = null;
        if (form.getParentId() != null) {
            parent = categories.findById(form.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("The selected parent category does not exist."));
        }

        try {
            return categories.saveAndFlush(Category.builder()
                    .name(name)
                    .parent(parent)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A category with this name already exists.", ex);
        }
    }
}
