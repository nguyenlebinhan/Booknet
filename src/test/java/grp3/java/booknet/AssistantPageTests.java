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
import grp3.java.booknet.service.assistant.AssistantService;
import grp3.java.booknet.service.assistant.AssistantTurn;
import grp3.java.booknet.service.assistant.AssistantUnavailableException;
import grp3.java.booknet.service.assistant.GeminiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC09 AI book assistant pages and rules. Gemini itself is replaced, so nothing leaves the machine. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssistantPageTests {
    private static int nextAddress = 1;

    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired UserRepository users;
    @MockitoBean GeminiClient gemini;

    private final MockHttpSession session = new MockHttpSession();
    /** Each test is its own guest, because allowances are counted per address for the whole run. */
    private final String address = "203.0.113." + nextAddress++;

    @Test
    void thePanelGreetsGuestsWithStartersAndTheirAllowance() throws Exception {
        connected();
        assertThat(page()).contains("id=\"ai-panel\"", "Hello! I can help you find a book",
                "How do I borrow a book?", "5 of 5 guest questions left today", "name=\"_csrf\"", "Answers only about booknet")
                .doesNotContain("id=\"ai-widget\"", "Sample conversation");
    }

    @Test
    void theBubbleIsOnlyForGuestsAndMembersAndNotInTheNavbarOrStaffLayouts() throws Exception {
        for (String path : List.of("/", "/catalog", "/about", "/contact")) {
            assertThat(html(get(path))).as(path).containsOnlyOnce("id=\"ai-widget\"").contains("data-url=\"/assistant\"")
                    .doesNotContain(">AI assistant</a>");
        }
        for (String path : List.of("/login", "/register", "/forgot-password", "/reset-password?token=x")) {
            assertThat(html(get(path))).as(path).doesNotContain("ai-widget");
        }
        CustomUserDetails member = new CustomUserDetails(account("bubble@example.com", UserRole.ROLE_USER));
        for (String path : List.of("/catalog", "/member/my-books", "/member/reservations", "/member/notifications",
                "/account/profile", "/account/change-password")) {
            assertThat(html(get(path).with(user(member)))).as(path).containsOnlyOnce("id=\"ai-widget\"")
                    .doesNotContain(">AI assistant</a>");
        }
        CustomUserDetails staff = new CustomUserDetails(account("bubble-staff@example.com", UserRole.ROLE_LIBRARIAN));
        for (String path : List.of("/catalog", "/staff/loans", "/admin/categories")) {
            assertThat(html(get(path).with(user(staff)))).as(path).doesNotContain("id=\"ai-widget\"", "/js/assistant.js")
                    .containsOnlyOnce("class=\"admin-sidebar\"");
        }
        CustomUserDetails admin = new CustomUserDetails(account("bubble-admin@example.com", UserRole.ROLE_ADMIN));
        for (String path : List.of("/admin/dashboard", "/admin/users", "/admin/roles", "/admin/tracking", "/admin/settings")) {
            String rendered = html(get(path).with(user(admin)));
            assertThat(rendered).as(path).doesNotContain("id=\"ai-widget\"", "/js/assistant.js")
                    .containsPattern("</aside>\\s*<(?:div class=\"(?:admin|mg)-workspace\"|main class=\"ad-page\")");
            write("layout-" + path.substring(7), rendered);
        }
    }

    @Test
    void staffCannotOpenSendOrResetAssistantByDirectUrl() throws Exception {
        for (UserRole role : List.of(UserRole.ROLE_ADMIN, UserRole.ROLE_LIBRARIAN)) {
            var staff = new CustomUserDetails(account("no-ai-" + role.name() + "@example.com", role));
            mvc.perform(get("/assistant").with(user(staff))).andExpect(status().isForbidden());
            mvc.perform(post("/assistant").with(user(staff)).with(csrf()).param("message", "Hi"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/assistant").with(user(staff)).with(csrf()).param("reset", "true"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(gemini);
    }

    @Test
    void showsTheAnswerWithRealBooksOnlyAndNeverAnInventedOrHiddenOne() throws Exception {
        connected();
        Book match = book("Childhood river story", PublicationStatus.PUBLISHED);
        Book draft = book("Childhood secret draft", PublicationStatus.DRAFT);
        answer(new GeminiClient.Answer(true, "Try <b>this</b> one.", List.of(match.getId(), draft.getId(), 999_999L, match.getId())));

        ask("  Stories about childhood? <i>please</i> ");
        String html = page();
        assertThat(html).contains("Try &lt;b&gt;this&lt;/b&gt; one.", "Childhood river story", "&lt;i&gt;please&lt;/i&gt;",
                "href=\"/catalog/" + match.getId() + "\"", "4 of 5 guest questions left today")
                .doesNotContain("Childhood secret draft", "<b>this</b>", "/catalog/999999", "Sample conversation");
        assertThat(html.split("class=\"ai-book\"", -1)).hasSize(2);

        ArgumentCaptor<String> rules = ArgumentCaptor.forClass(String.class);
        verify(gemini).ask(rules.capture(), any(), anyString());
        assertThat(rules.getValue()).contains("Answer only about Booknet", "id=" + match.getId() + " | Childhood river story",
                "up to 5 books at once, 14 days each", "5,000 VND per book").doesNotContain("Childhood secret draft");
        write("assistant", html);
    }

    @Test
    void offTopicQuestionsGetTheFixedReplyWhateverTheModelWrote() throws Exception {
        connected();
        Book book = book("Off topic book", PublicationStatus.PUBLISHED);
        answer(new GeminiClient.Answer(false, "Here is some Python code", List.of(book.getId())));
        ask("Write Python code for me");
        assertThat(page()).contains("I can only help with Booknet", "4 of 5 guest questions left today")
                .doesNotContain("Here is some Python code", "class=\"ai-book\"");

        // The refused turn is not given to the model as context for the next question.
        ask("And now?");
        verify(gemini, times(2)).ask(anyString(), org.mockito.ArgumentMatchers.eq(List.of()), anyString());
    }

    @Test
    void guestsAreLimitedPerAddressEvenWithNewSessionsAndMembersAreNot() throws Exception {
        connected();
        answer(new GeminiClient.Answer(true, "Hello reader", List.of()));
        for (int i = 0; i < 5; i++) {
            mvc.perform(from(post("/assistant")).with(csrf()).session(new MockHttpSession()).param("message", "Hi"))
                    .andExpect(redirectedUrl("/assistant"));
        }
        String refused = mvc.perform(from(post("/assistant")).with(csrf()).session(session).param("message", "My sixth question"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(refused).contains("You have used today&#39;s 5 guest questions. Sign in to keep asking",
                "My sixth question", "0 of 5 guest questions left today");
        verify(gemini, times(5)).ask(anyString(), any(), anyString());

        User member = account("quota@example.com", UserRole.ROLE_USER);
        for (int i = 0; i < 7; i++) {
            mvc.perform(from(post("/assistant")).with(csrf()).with(user(new CustomUserDetails(member))).param("message", "Hi"))
                    .andExpect(redirectedUrl("/assistant"));
        }
        verify(gemini, times(12)).ask(anyString(), any(), anyString());
    }

    @Test
    void whenGeminiFailsTheGuestKeepsTheQuestionAndGetsCatalogMatches() throws Exception {
        connected();
        Book match = book("Lighthouse keeper", PublicationStatus.PUBLISHED);
        book("Unrelated title", PublicationStatus.PUBLISHED);
        when(gemini.ask(anyString(), any(), anyString())).thenThrow(new AssistantUnavailableException("HTTP 429"));
        ask("a lighthouse");
        assertThat(page()).contains("cannot answer right now", "Search the catalog for your question", "Lighthouse keeper",
                "href=\"/catalog/" + match.getId() + "\"", "5 of 5 guest questions left today")
                .doesNotContain("Unrelated title", "HTTP 429");
    }

    @Test
    void withoutAKeyThePageSaysSoAndOffersTheCatalog() throws Exception {
        when(gemini.ask(anyString(), any(), anyString())).thenThrow(new AssistantUnavailableException("No key"));
        for (int i = 0; i < 6; i++) {
            ask("question " + i);
        }
        assertThat(page()).contains("AI is not connected yet", "cannot answer right now")
                .doesNotContain("guest questions left", "Powered by");
    }

    @Test
    void followUpsCarryTheConversationAndNewConversationClearsIt() throws Exception {
        connected();
        answer(new GeminiClient.Answer(true, "First answer", List.of()));
        ask("First question");
        ask("Second question");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssistantTurn>> history = ArgumentCaptor.forClass(List.class);
        verify(gemini, times(2)).ask(anyString(), history.capture(), anyString());
        assertThat(history.getAllValues().get(0)).isEmpty();
        assertThat(history.getAllValues().get(1)).extracting(AssistantTurn::question).containsExactly("First question");

        mvc.perform(from(post("/assistant")).session(session).with(csrf()).param("reset", "true"))
                .andExpect(redirectedUrl("/assistant"));
        assertThat(page()).doesNotContain("First question", "First answer").contains("Hello! I can help you find a book");
    }

    @Test
    void contactDetailsInAQuestionAreRemovedBeforeItIsSent() throws Exception {
        connected();
        answer(new GeminiClient.Answer(true, "Ok", List.of()));
        ask("I am me@mail.example.com, call 0912 345 678. Is 978-604-1-22345-6 here?");
        ArgumentCaptor<String> question = ArgumentCaptor.forClass(String.class);
        verify(gemini).ask(anyString(), any(), question.capture());
        assertThat(question.getValue()).isEqualTo("I am [email removed], call [phone removed]. Is 978-604-1-22345-6 here?");
    }

    @Test
    void emptyTooLongAndForgedMessagesNeverReachGemini() throws Exception {
        connected();
        mvc.perform(from(post("/assistant")).param("message", "Hello")).andExpect(status().isForbidden());
        assertThat(error("   ")).contains("Type a message before sending.");
        assertThat(error("x".repeat(1001))).contains("Keep your message under " + AssistantService.MAX_MESSAGE + " characters.");
        verify(gemini, never()).ask(anyString(), any(), anyString());
    }

    @Test
    void membersHaveNoLimitStaffKeepTheSidebarAndNeitherSendsAccountData() throws Exception {
        connected();
        answer(new GeminiClient.Answer(true, "Ok", List.of()));
        User member = account("assistant@example.com", UserRole.ROLE_USER);
        User staff = account("assistant-staff@example.com", UserRole.ROLE_LIBRARIAN);
        assertThat(mvc.perform(get("/assistant").with(user(new CustomUserDetails(member)))).andReturn().getResponse().getContentAsString())
                .contains("bn-navbar", "id=\"ai-panel\"")
                .doesNotContain("guest questions left");
        mvc.perform(get("/assistant").with(user(new CustomUserDetails(staff)))).andExpect(status().isForbidden());
        mvc.perform(post("/assistant").with(csrf()).with(user(new CustomUserDetails(member))).param("message", "What do I owe?"));
        ArgumentCaptor<String> rules = ArgumentCaptor.forClass(String.class);
        verify(gemini).ask(rules.capture(), any(), anyString());
        assertThat(rules.getValue()).doesNotContain("assistant@example.com", member.getFullName());
    }

    private void connected() {
        when(gemini.isConfigured()).thenReturn(true);
    }

    private void answer(GeminiClient.Answer answer) {
        when(gemini.ask(anyString(), any(), anyString())).thenReturn(answer);
    }

    private void ask(String message) throws Exception {
        mvc.perform(from(post("/assistant")).session(session).with(csrf()).param("message", message))
                .andExpect(redirectedUrl("/assistant"));
    }

    private String error(String message) throws Exception {
        return mvc.perform(from(post("/assistant")).session(session).with(csrf()).param("message", message))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String page() throws Exception {
        return mvc.perform(from(get("/assistant")).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String html(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(from(request)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder request) {
        return request.with(raw -> {
            raw.setRemoteAddr(address);
            return raw;
        });
    }

    private Book book(String title, PublicationStatus status) {
        return books.save(Book.builder().title(title).publicationStatus(status).build());
    }

    private User account(String email, UserRole role) {
        return users.save(User.builder().fullName("Assistant Test Person").email(email).role(role)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
    }

    /** Saves the rendered page under target/ui-review for a visual check. */
    private static void write(String name, String html) throws Exception {
        Path output = Path.of("target", "ui-review");
        Files.createDirectories(output);
        String assets = Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/").replace("src=\"/js/", "src=\"" + assets + "js/"));
    }
}
