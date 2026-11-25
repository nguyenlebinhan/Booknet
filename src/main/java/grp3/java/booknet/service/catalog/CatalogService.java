package grp3.java.booknet.service.catalog;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.entity.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface CatalogService {

    Page<CatalogBookView> search(CatalogFilter filter, Pageable pageable, boolean includeUnpublished);

    /** Published, undeleted books with these IDs, in the order given. */
    List<CatalogBookView> findPublished(Collection<Long> bookIds);

    List<Category> getCategories();

    List<Short> getPublishYears(boolean includeUnpublished);
}
