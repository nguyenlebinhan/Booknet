package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "parent")
    List<Category> findAllByOrderByNameAsc();

    @Override
    @EntityGraph(attributePaths = "parent")
    Page<Category> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "parent")
    List<Category> findAllById(Iterable<Integer> ids);

    @EntityGraph(attributePaths = "parent")
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    List<Category> findAllByParent_IdInOrderByNameAsc(Collection<Integer> parentIds);

    @Query("select distinct category.parent.id from Category category where category.parent.id in :parentIds")
    Set<Integer> findParentIdsWithChildren(@Param("parentIds") Collection<Integer> parentIds);
}
