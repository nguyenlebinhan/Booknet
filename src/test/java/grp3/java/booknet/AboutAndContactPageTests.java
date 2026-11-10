package grp3.java.booknet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AboutAndContactPageTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void aboutPageRendersCorrectContentAndDesignSections() throws Exception {
        mvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"))
                .andExpect(model().attribute("activePage", "about"))
                .andExpect(content().string(containsString("About booknet")))
                .andExpect(content().string(containsString("A shared shelf.")))
                .andExpect(content().string(containsString("A world of possibilities.")))
                .andExpect(content().string(containsString("A book opens. A world stays with you.")))
                .andExpect(content().string(containsString("Explore the catalog")))
                .andExpect(content().string(containsString("12,000+")))
                .andExpect(content().string(containsString("carefully selected titles")))
                .andExpect(content().string(containsString("WHAT WE BELIEVE")))
                .andExpect(content().string(containsString("Curiosity comes first")))
                .andExpect(content().string(containsString("Reading is for everyone")))
                .andExpect(content().string(containsString("Shared books, lasting care")))
                .andExpect(content().string(containsString("YOUR READING JOURNEY")))
                .andExpect(content().string(containsString("Become a member")))
                .andExpect(content().string(containsString("Find your next book")))
                .andExpect(content().string(containsString("Read, return, repeat")))
                .andExpect(content().string(containsString("There is a place for you here.")))
                .andExpect(content().string(containsString("Contact booknet")))
                .andExpect(content().string(containsString("figma-reading-room.png")));
    }

    @Test
    void contactPageRendersCorrectContentAndForm() throws Exception {
        mvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("contact"))
                .andExpect(model().attribute("activePage", "contact"))
                .andExpect(content().string(containsString("GET IN TOUCH")))
                .andExpect(content().string(containsString("A question? A reading recommendation?")))
                .andExpect(content().string(containsString("We are here to help.")))
                .andExpect(content().string(containsString("Send us a message")))
                .andExpect(content().string(containsString("name=\"fullName\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"subject\"")))
                .andExpect(content().string(containsString("name=\"memberId\"")))
                .andExpect(content().string(containsString("name=\"message\"")))
                .andExpect(content().string(containsString("Design preview — no message will be sent.")))
                .andExpect(content().string(containsString("Let us point you")))
                .andExpect(content().string(containsString("hello@booknet.vn")))
                .andExpect(content().string(containsString("024 3822 2026")))
                .andExpect(content().string(containsString("28 Nguyễn Du, Hai Bà Trưng, Hà Nội")))
                .andExpect(content().string(containsString("Already a member?")))
                .andExpect(content().string(containsString("View my loans")))
                .andExpect(content().string(containsString("COME BY BOOKNET")))
                .andExpect(content().string(containsString("BEFORE YOU ASK")))
                .andExpect(content().string(containsString("How do I get a member card?")))
                .andExpect(content().string(containsString("Can I renew a book?")))
                .andExpect(content().string(containsString("What if my book is overdue?")))
                .andExpect(content().string(containsString("figma-reading-room.png")));
    }

    @Test
    void contactFormSubmissionRedirectsWithSuccessFlash() throws Exception {
        mvc.perform(post("/contact")
                        .with(csrf())
                        .param("fullName", "Nguyen Van A")
                        .param("email", "vana@example.com")
                        .param("subject", "General enquiry")
                        .param("memberId", "TV-02481")
                        .param("message", "Can I suggest a new book for the library?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(flash().attributeExists("contactSuccess"));
    }
}

