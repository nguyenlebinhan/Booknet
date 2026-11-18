package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Optional<Author> findByName(String name);

    Optional<Author> findFirstByNameIgnoreCase(String name);

    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Author> findByNameStartingWithIgnoreCase(String prefix, Pageable pageable);

    Page<Author> findByNameStartingWithIgnoreCaseAndNameContainingIgnoreCase(
            String prefix, String query, Pageable pageable);

    @Query("select distinct upper(substring(author.name, 1, 1)) from Author author "
            + "where author.name is not null order by upper(substring(author.name, 1, 1))")
    List<String> findDistinctInitials();
}
