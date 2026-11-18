package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book.PublisherRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
class AdminBookControllerFeature2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("Admin can display Add Book form")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void adminCanDisplayAddBookForm() throws Exception {
        mockMvc.perform(get("/admin/books/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/create"))
                .andExpect(model().attributeExists("bookForm"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("authors"))
                .andExpect(model().attributeExists("publishers"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"admin-sidebar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ADMIN SPACE")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"booknet-navbar\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Book code *")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"id\""))));
    }

    @Test
    @DisplayName("Librarian can display the add book form")
    @WithMockUser(username = "librarian@booknet.vn", authorities = {"ROLE_LIBRARIAN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void librarianCanDisplayAddBookForm() throws Exception {
        mockMvc.perform(get("/admin/books/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/create"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"admin-sidebar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STAFF SPACE")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"booknet-navbar\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Add a book")));
    }

    @Test
    @DisplayName("Admin can submit valid book creation form and be redirected with flash message")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void adminCanSubmitValidBookForm() throws Exception {
        when(bookService.createBook(any(BookCreateForm.class))).thenReturn(100L);

        mockMvc.perform(post("/admin/books/create")
                        .with(csrf())
                        .param("title", "Clean Architecture")
                        .param("bookCode", "BK-ARCH-01")
                        .param("isbn", "978-0134494166")
                        .param("authorIds", "1")
                        .param("categoryIds", "1")
                        .param("language", "en")
                        .param("publishYear", "2017")
                        .param("pageCount", "432"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/books/100/edit"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Validation fails when required fields are missing and returns form view with errors")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void validationErrorsPreserveFormView() throws Exception {
        mockMvc.perform(post("/admin/books/create")
                        .with(csrf())
                        .param("title", "") // blank title
                        .param("bookCode", "invalid code with spaces!") // invalid pattern
                        .param("isbn", "invalid-isbn")) // invalid isbn
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("bookForm", "title", "bookCode", "isbn", "authorIds", "categoryIds", "publishYear", "pageCount"));
    }

    @Test
    @DisplayName("Business duplicate error is captured and displayed in globalError attribute")
    @WithMockUser(username = "admin@booknet.vn", authorities = {"ROLE_ADMIN","CATALOG_EDIT","COPY_MANAGE","BOOK_RETIRE"})
    void duplicateCodeCapturedAsGlobalError() throws Exception {
        when(bookService.createBook(any(BookCreateForm.class)))
                .thenThrow(new IllegalArgumentException("Book code 'BK-ARCH-01' is already in use"));

        mockMvc.perform(post("/admin/books/create")
                        .with(csrf())
                        .param("title", "Clean Architecture")
                        .param("bookCode", "BK-ARCH-01")
                        .param("isbn", "978-0134494166")
                        .param("authorIds", "1")
                        .param("categoryIds", "1")
                        .param("language", "en")
                        .param("publishYear", "2017")
                        .param("pageCount", "432"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/create"))
                .andExpect(model().attribute("globalError", "Book code 'BK-ARCH-01' is already in use"));
    }
}
