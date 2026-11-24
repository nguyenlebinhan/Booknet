package grp3.java.booknet.service.catalog;

import grp3.java.booknet.dto.catalog.CategoryCreateForm;
import grp3.java.booknet.entity.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CategoryService {

    List<Category> listCategories();

    Page<Category> listCategories(Pageable pageable);

    Page<Category> searchCategories(String name, Pageable pageable);

    List<Category> listDescendants(Integer categoryId);

    Set<Integer> findParentIdsWithChildren(Collection<Integer> categoryIds);

    Category createCategory(CategoryCreateForm form);
}
