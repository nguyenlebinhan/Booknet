package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.dto.book.response.BookListRowDto;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBookControllerFeature1Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("Admin user can access book management list page")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void adminCanAccessBookListPage() throws Exception {
        BookListRowDto dummyRow = BookListRowDto.builder()
                .id(1L)
                .bookCode("BK-001")
                .isbn("978-1111111111")
                .title("Spring Boot in Action")
                .authors(List.of("Craig Walls"))
                .categories(List.of("Technology"))
                .publisherName("Manning")
                .publishYear((short) 2016)
                .language("en")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(false)
                .totalCopies(3)
                .availableCopies(2)
                .build();

        when(bookService.getBookManagementList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(dummyRow), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/list"))
                .andExpect(model().attributeExists("booksPage"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("publicationStatuses"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalElements", 1L));
    }

    @Test
    @DisplayName("Anonymous user is redirected to login when accessing admin books")
    void anonymousUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Regular user without ADMIN role is denied access (403)")
    @WithMockUser(username = "member@booknet.vn", roles = {"USER"})
    void regularUserForbidden() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid pagination parameters are normalized safely")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void invalidPaginationParametersNormalized() throws Exception {
        when(bookService.getBookManagementList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/admin/books")
                        .param("page", "-5")
                        .param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books/list"))
                .andExpect(model().attribute("currentPage", 0));
    }
}
