package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBookControllerFeature3Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("Admin can display edit form for existing book")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void adminCanDisplayEditBookForm() throws Exception {
        BookEditForm dummyForm = BookEditForm.builder()
                .title("Spring in Action")
                .bookCode("BK-SIA-01")
                .isbn("978-1617294945")
                .authorIds(List.of(1L))
                .categoryIds(List.of(1L))
                .publishYear((short) 2018)
                .pageCount(520)
                .build();

        when(bookService.getBookForEdit(1L)).thenReturn(dummyForm);

        mockMvc.perform(get("/admin/books/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/edit"))
                .andExpect(model().attributeExists("bookForm"))
                .andExpect(model().attribute("bookId", 1L))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("authors"))
                .andExpect(model().attributeExists("publishers"));
    }

    @Test
    @DisplayName("Redirects to books list with error message when book ID not found for edit")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void editNonExistentBookRedirects() throws Exception {
        when(bookService.getBookForEdit(999L)).thenThrow(new IllegalArgumentException("Book not found with ID: 999"));

        mockMvc.perform(get("/admin/books/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/books"))
                .andExpect(flash().attribute("errorMessage", "Book not found with ID: 999"));
    }

    @Test
    @DisplayName("Admin can successfully submit valid book update and be redirected")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void adminCanSubmitValidBookUpdate() throws Exception {
        mockMvc.perform(post("/admin/books/1/edit")
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
                .andExpect(redirectedUrl("/admin/books"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Validation failure on edit preserves form view with errors")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void editValidationFailurePreservesView() throws Exception {
        mockMvc.perform(post("/admin/books/1/edit")
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
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void editConflictCapturedInGlobalError() throws Exception {
        doThrow(new IllegalArgumentException("Book code 'BK-CONFLICT' is already in use by another book"))
                .when(bookService).updateBook(eq(1L), any(BookEditForm.class));

        mockMvc.perform(post("/admin/books/1/edit")
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
