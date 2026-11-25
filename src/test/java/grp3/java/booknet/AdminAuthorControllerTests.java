package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.CoverImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuthorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorRepository authors;

    @Autowired
    private BookRepository books;

    @Autowired
    private CoverImageStorage imageStorage;

    private final List<String> storedAuthorImages = new ArrayList<>();

    @AfterEach
    void removeStoredAuthorImages() {
        storedAuthorImages.forEach(imageStorage::delete);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateAuthorAndSeeItInAuthorList() throws Exception {
        mockMvc.perform(post("/admin/authors/create")
                        .with(csrf())
                        .param("name", "  Ursula Le Guin  ")
                        .param("bio", "American author"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/authors"))
                .andExpect(flash().attributeExists("successMessage"));

        Author saved = authors.findFirstByNameIgnoreCase("Ursula Le Guin").orElseThrow();
        assertEquals("Ursula Le Guin", saved.getName());
        assertEquals("American author", saved.getBio());

        mockMvc.perform(get("/admin/authors").param("letter", "U"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/authors/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ursula Le Guin")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateAuthorNameReturnsCreateFormWithValidationError() throws Exception {
        authors.save(Author.builder().name("Ursula Le Guin").build());

        mockMvc.perform(post("/admin/authors/create")
                        .with(csrf())
                        .param("name", " ursula le guin "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/authors/create"))
                .andExpect(model().attributeHasFieldErrors("authorForm", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFormAcceptsAnOptionalMultipartPhoto() throws Exception {
        mockMvc.perform(get("/admin/authors/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/authors/create"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"image\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("image/jpeg,image/png")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorListIsPaginatedAndSearchable() throws Exception {
        for (int index = 0; index < 23; index++) {
            authors.save(Author.builder().name("Author paging " + index).build());
        }

        mockMvc.perform(get("/admin/authors").param("page", "0").param("query", "Author paging"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("authorsPage",
                        org.hamcrest.Matchers.hasProperty("numberOfElements", org.hamcrest.Matchers.is(20))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Search by author name")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Sort by"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("authorSort"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Browse authors by initial\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("author-directory-avatar")));

        mockMvc.perform(get("/admin/authors").param("page", "1").param("query", "Author paging"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Author paging 7")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page 2 of 2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("letter=A")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorListDefaultsToAAndOnlyDisplaysSelectedInitial() throws Exception {
        authors.save(Author.builder().name("Ada Lovelace").build());
        authors.save(Author.builder().name("Beatrix Potter").build());
        authors.save(Author.builder().name("Charles Dickens").build());

        mockMvc.perform(get("/admin/authors"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedLetter", "A"))
                .andExpect(model().attribute("authorsPage",
                        org.hamcrest.Matchers.hasProperty("totalElements", org.hamcrest.Matchers.is(1L))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ada Lovelace")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Beatrix Potter"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Charles Dickens"))));

        mockMvc.perform(get("/admin/authors").param("letter", "B"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedLetter", "B"))
                .andExpect(model().attribute("authorsPage",
                        org.hamcrest.Matchers.hasProperty("totalElements", org.hamcrest.Matchers.is(1L))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Beatrix Potter")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Ada Lovelace"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorPhotoUploadIsStoredAndShownOnAuthorPages() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "portrait.png", "image/png", validPng());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/admin/authors/create")
                        .file(image)
                        .with(csrf())
                        .param("name", "Portrait Author"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/authors"));

        Author saved = authors.findFirstByNameIgnoreCase("Portrait Author").orElseThrow();
        assertTrue(saved.getUrl().matches("/covers/[0-9a-f-]{36}\\.png"));
        storedAuthorImages.add(saved.getUrl());

        mockMvc.perform(get(saved.getUrl()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        mockMvc.perform(get("/admin/authors").param("letter", "P"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"" + saved.getUrl() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-author-photo-src=\"" + saved.getUrl() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-author-photo-dialog")));

        mockMvc.perform(get("/admin/authors/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"" + saved.getUrl() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-author-photo-src=\"" + saved.getUrl() + "\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorPhotoUploadRejectsInvalidImageWithFieldError() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "portrait.png", "image/png", "not an image".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/admin/authors/create")
                        .file(image)
                        .with(csrf())
                        .param("name", "Invalid Portrait"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/authors/create"))
                .andExpect(model().attributeHasFieldErrors("authorForm", "image"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Only JPEG and PNG author photos are supported.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorCardsLinkToTheirBookDirectory() throws Exception {
        Author author = authors.save(Author.builder().name("Clickable Author").build());

        mockMvc.perform(get("/admin/authors").param("letter", "C"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/admin/authors/" + author.getId() + "\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorBookDirectorySearchesSortsAndPaginatesBooks() throws Exception {
        Author author = authors.save(Author.builder().name("Directory Author").bio("A short biography.").build());
        Author otherAuthor = authors.save(Author.builder().name("Another Author").build());
        for (int index = 1; index <= 13; index++) {
            books.save(Book.builder()
                    .title(String.format("Directory Book %02d", index))
                    .bookCode(String.format("DIR-%03d", index))
                    .isbn(String.format("978000009%04d", index))
                    .publishYear((short) (2010 + index))
                    .publicationStatus(PublicationStatus.PUBLISHED)
                    .authors(java.util.Set.of(author))
                    .build());
        }
        books.save(Book.builder()
                .title("Unrelated author book")
                .bookCode("DIR-OTHER")
                .isbn("9780000099999")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .authors(java.util.Set.of(otherAuthor))
                .build());

        String descendingHtml = mockMvc.perform(get("/admin/authors/{id}", author.getId())
                        .param("sort", "title-desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/authors/details"))
                .andExpect(model().attribute("booksPage",
                        org.hamcrest.Matchers.hasProperty("totalElements", org.hamcrest.Matchers.is(13L))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Directory Author")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A short biography.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Unrelated author book"))))
                .andReturn().getResponse().getContentAsString();
        assertTrue(descendingHtml.indexOf("Directory Book 13") < descendingHtml.indexOf("Directory Book 12"));

        mockMvc.perform(get("/admin/authors/{id}", author.getId())
                        .param("query", "Directory Book 01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("booksPage",
                        org.hamcrest.Matchers.hasProperty("totalElements", org.hamcrest.Matchers.is(1L))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Directory Book 01")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Directory Book 02"))));

        mockMvc.perform(get("/admin/authors/{id}", author.getId())
                        .param("page", "1")
                        .param("sort", "title-asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Directory Book 13")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page 2 of 2")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Directory Book 12"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorBookDirectoryReturnsNotFoundForUnknownAuthor() throws Exception {
        mockMvc.perform(get("/admin/authors/{id}", Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_LIBRARIAN", "CATALOG_EDIT"})
    void librarianCanSearchAuthorsForBookFormsAndManageAuthors() throws Exception {
        authors.save(Author.builder().name("Searchable Writer").build());

        mockMvc.perform(get("/api/staff/authors/options").param("query", "Searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors[0].name").value("Searchable Writer"));

        mockMvc.perform(get("/admin/authors"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void memberCannotManageAuthors() throws Exception {
        mockMvc.perform(get("/admin/authors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorCreationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/authors/create").param("name", "No CSRF Writer"))
                .andExpect(status().isForbidden());
    }

    private static byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
