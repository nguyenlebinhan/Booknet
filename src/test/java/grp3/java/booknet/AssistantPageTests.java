package grp3.java.booknet;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC09 AI book assistant, UI only: no AI service is called and nothing is stored. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssistantPageTests {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired UserRepository users;

    @Test
    void guestsSeeTheSampleConversationWithRealBooks() throws Exception {
        Book book = books.save(Book.builder().title("Assistant sample book").publicationStatus(PublicationStatus.PUBLISHED).build());
        String html = mvc.perform(get("/assistant")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("One conversation. A new book.", "Sample experience", "UI example · AI is not connected",
                "Assistant sample book", "href=\"/catalog/" + book.getId() + "\"", "Where would you like to start?",
                "name=\"_csrf\"", "href=\"/assistant\"", "AI assistant", "Sign in</a> to check your own loans")
                .doesNotContain("For your account:");
    }

    @Test
    void sendingAMessageShowsItWithTheFixedReplyAndASearchLink() throws Exception {
        String html = mvc.perform(post("/assistant").with(csrf()).param("message", "  Books about <b>childhood</b>  "))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Books about &lt;b&gt;childhood&lt;/b&gt;", "The AI assistant is not connected yet",
                "Search the catalog for your question").doesNotContain("<b>childhood</b>");
        write("assistant", html);

        mvc.perform(post("/assistant").param("message", "Hello")).andExpect(status().isForbidden());
        assertThat(mvc.perform(post("/assistant").with(csrf()).param("message", "   "))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .contains("Type a message before sending.").doesNotContain("Search the catalog for your question");
        assertThat(mvc.perform(post("/assistant").with(csrf()).param("message", "x".repeat(1001)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .contains("Keep your message under 1000 characters.");
    }

    /** Saves the rendered page under target/ui-review for a visual check. */
    private static void write(String name, String html) throws Exception {
        java.nio.file.Path output = java.nio.file.Path.of("target", "ui-review");
        java.nio.file.Files.createDirectories(output);
        String assets = java.nio.file.Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        java.nio.file.Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/").replace("src=\"/js/", "src=\"" + assets + "js/"));
    }

    @Test
    void signedInReadersSeeTheirAccountAndStaffKeepTheSidebar() throws Exception {
        User member = users.save(User.builder().fullName("Assistant Reader").email("assistant@example.com").role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
        User staff = users.save(User.builder().fullName("Assistant Staff").email("assistant-staff@example.com").role(UserRole.ROLE_LIBRARIAN)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
        assertThat(mvc.perform(get("/assistant").with(user(new CustomUserDetails(member)))).andReturn().getResponse().getContentAsString())
                .contains("For your account:", "assistant@example.com", "bn-navbar");
        assertThat(mvc.perform(get("/assistant").with(user(new CustomUserDetails(staff)))).andReturn().getResponse().getContentAsString())
                .contains("admin-sidebar").doesNotContain("bn-navbar");
    }
}
