package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    Optional<Publisher> findByName(String name);

    Optional<Publisher> findByNameIgnoreCase(String name);
}
