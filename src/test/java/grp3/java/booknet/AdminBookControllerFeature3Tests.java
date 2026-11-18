package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.dto.book.response.BookDeletionCheckDto;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.BookLifecycleService;
import grp3.java.booknet.service.book.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
class AdminBookControllerFeature3Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private BookCopyRepository bookCopyRepository;

    @MockitoBean
    private BookLifecycleService bookLifecycleService;

    @org.junit.jupiter.api.BeforeEach
    void setUpEditPageData() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Spring in Action");
        book.setBookCode("BK-SIA-01");
        book.setPublicationStatus(PublicationStatus.DRAFT);
        lenient().when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        lenient().when(bookCopyRepository.findByBook_Id(1L)).thenReturn(List.of());
        lenient().when(bookLifecycleService.checkHardDelete(1L)).thenReturn(BookDeletionCheckDto.builder()
                .bookId(1L).bookCode(book.getBookCode()).title(book.getTitle()).hardDeleteAllowed(true).build());
    }

    @Test
    @DisplayName("Admin can display edit form for existing book")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void adminCanDisplayEditBookForm() throws Exception {
        BookEditForm dummyForm = BookEditForm.builder()
                .title("Spring in Action")
                .bookCode("BK-SIA-01")
                .isbn("978-1617294945")
                .authorIds(List.of(1))
                .categoryIds(List.of(1))
                .publishYear((short) 2018)
                .pageCount(520)
                .build();

        when(bookService.getBookForEdit(1L)).thenReturn(dummyForm);

        mockMvc.perform(get("/admin/books/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Basic information")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Copies &amp; shelf locations")))
                .andExpect(model().attributeExists("bookForm"))
                .andExpect(model().attribute("bookId", 1L))
                .andExpect(model().attributeExists("selectedCategories"))
                .andExpect(model().attributeExists("selectedAuthors"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Category *")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Authors *")));
    }

    @Test
    @DisplayName("Librarian can view the book edit page")
    @WithMockUser(username = "librarian@booknet.vn", authorities = {"ROLE_LIBRARIAN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void librarianCanDisplayEditBookForm() throws Exception {
        when(bookService.getBookForEdit(1L)).thenReturn(BookEditForm.builder()
                .title("Spring in Action")
                .bookCode("BK-SIA-01")
                .isbn("978-1617294945")
                .authorIds(List.of(1))
                .categoryIds(List.of(1))
                .publishYear((short) 2018)
                .pageCount(520)
                .build());

        mockMvc.perform(get("/admin/books/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/edit"));
    }

    @Test
    @DisplayName("Redirects to catalog with error message when book ID not found for edit")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void editNonExistentBookRedirects() throws Exception {
        when(bookService.getBookForEdit(999L)).thenThrow(new IllegalArgumentException("Book not found with ID: 999"));

        mockMvc.perform(get("/admin/books/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog"))
                .andExpect(flash().attribute("errorMessage", "Book not found with ID: 999"));
    }

    @Test
    @DisplayName("Admin can successfully submit valid book update and be redirected")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void adminCanSubmitValidBookUpdate() throws Exception {
        mockMvc.perform(multipart("/admin/books/1/edit")
                        .with(csrf())
                        .param("title", "Spring in Action 5th Edition")
                        .param("bookCode", "BK-SIA-05")
                        .param("isbn", "978-1617294945")
                        .param("authorIds", "1")
                        .param("categoryIds", "1")
                        .param("language", "en")
                        .param("publishYear", "2018")
                        .param("pageCount", "520"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/books/1/edit"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Librarian can save and publish an edited book")
    @WithMockUser(username = "librarian@booknet.vn", authorities = {"ROLE_LIBRARIAN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void librarianCanPublishEditedBook() throws Exception {
        mockMvc.perform(multipart("/admin/books/1/publish")
                        .with(csrf())
                        .param("title", "Spring in Action")
                        .param("bookCode", "BK-SIA-01")
                        .param("isbn", "978-1617294945")
                        .param("authorIds", "1")
                        .param("categoryIds", "1")
                        .param("language", "en")
                        .param("publishYear", "2018")
                        .param("pageCount", "520"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/books/1/edit"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(bookService).updateAndPublishBook(eq(1L), any(BookEditForm.class));
    }

    @Test
    @DisplayName("Validation failure on edit preserves form view with errors")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void editValidationFailurePreservesView() throws Exception {
        mockMvc.perform(multipart("/admin/books/1/edit")
                        .with(csrf())
                        .param("title", "") // blank title
                        .param("bookCode", "Invalid Space Code")
                        .param("isbn", "bad-isbn"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("bookForm", "title", "bookCode", "isbn"));
    }

    @Test
    @DisplayName("Conflict error on update is displayed via globalError attribute")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void editConflictCapturedInGlobalError() throws Exception {
        doThrow(new IllegalArgumentException("Book code 'BK-CONFLICT' is already in use by another book"))
                .when(bookService).updateBook(eq(1L), any(BookEditForm.class));

        mockMvc.perform(multipart("/admin/books/1/edit")
                        .with(csrf())
                        .param("title", "Spring in Action")
                        .param("bookCode", "BK-CONFLICT")
                        .param("isbn", "978-1617294945")
                        .param("authorIds", "1")
                        .param("categoryIds", "1")
                        .param("language", "en")
                        .param("publishYear", "2018")
                        .param("pageCount", "520"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/edit"))
                .andExpect(model().attribute("globalError", "Book code 'BK-CONFLICT' is already in use by another book"));
    }
}
