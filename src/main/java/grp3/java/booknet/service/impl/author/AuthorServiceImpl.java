package grp3.java.booknet.service.impl.author;

import grp3.java.booknet.dto.author.AuthorCreateForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.author.AuthorService;
import grp3.java.booknet.service.author.InvalidAuthorImageException;
import grp3.java.booknet.service.book.CoverImageStorage;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authors;
    private final BookRepository books;
    private final CoverImageStorage coverImageStorage;

    @Override
    @Transactional(readOnly = true)
    public Page<Author> listAuthors(String query, Pageable pageable) {
        String searchTerm = query == null ? "" : query.trim();
        return searchTerm.isEmpty()
                ? authors.findAll(pageable)
                : authors.findByNameContainingIgnoreCase(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Author> listAuthorsByInitial(String initial, String query, Pageable pageable) {
        String searchTerm = query == null ? "" : query.trim();
        return searchTerm.isEmpty()
                ? authors.findByNameStartingWithIgnoreCase(initial, pageable)
                : authors.findByNameStartingWithIgnoreCaseAndNameContainingIgnoreCase(initial, searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listAuthorInitials() {
        return authors.findDistinctInitials();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Author> findAuthor(Integer authorId) {
        return authors.findById(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> searchBooksByAuthor(Integer authorId, String query, Pageable pageable) {
        String searchTerm = query == null ? "" : query.trim();
        return books.searchByAuthor(authorId, searchTerm, pageable);
    }

    @Override
    @Transactional
    public Author createAuthor(AuthorCreateForm form) {
        String name = form.getName().trim();
        if (authors.findFirstByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("An author with this name already exists.");
        }

        String bio = form.getBio() == null || form.getBio().isBlank() ? null : form.getBio().trim();
        String imageUrl;
        try {
            imageUrl = coverImageStorage.store(form.getImage());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAuthorImageException(ex.getMessage(), ex);
        }

        try {
            return authors.saveAndFlush(Author.builder()
                    .name(name)
                    .bio(bio)
                    .url(imageUrl)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            deleteStoredImage(imageUrl, ex);
            throw new IllegalArgumentException("An author with this name already exists.", ex);
        } catch (RuntimeException ex) {
            deleteStoredImage(imageUrl, ex);
            throw ex;
        }
    }

    private void deleteStoredImage(String imageUrl, RuntimeException originalException) {
        if (imageUrl == null) {
            return;
        }
        try {
            coverImageStorage.delete(imageUrl);
        } catch (RuntimeException cleanupException) {
            log.error("Failed to remove author image {} after author creation failed.", imageUrl, cleanupException);
            originalException.addSuppressed(cleanupException);
        }
    }
}
