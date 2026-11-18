package grp3.java.booknet;

import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.repository.book.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
class AdminCategoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categories;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianCanCreateCategoryAndIsRedirectedToCategoryList() throws Exception {
        Category parent = categories.save(Category.builder().name("Fiction").build());

        mockMvc.perform(post("/admin/categories/create")
                        .with(csrf())
                        .param("name", "  Mystery  ")
                        .param("parentId", parent.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        categories.save(Category.builder().name("Fantasy").parent(parent).build());

        MvcResult result = mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertEquals(1, StringUtils.countOccurrencesOf(html, "<strong>Fiction</strong>"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "<strong>Mystery</strong>"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "<strong>Fantasy</strong>"));
        assertEquals(2, StringUtils.countOccurrencesOf(html, "data-depth=\"1\""));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateCategoryNameReturnsCreateFormWithFieldError() throws Exception {
        categories.save(Category.builder().name("Literature").build());

        mockMvc.perform(post("/admin/categories/create")
                        .with(csrf())
                        .param("name", " literature "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryFormReportsRequiredName() throws Exception {
        mockMvc.perform(post("/admin/categories/create")
                        .with(csrf())
                        .param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void memberCannotAccessCategoryManagement() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void guestMustSignInBeforeAccessingCategoryManagement() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryCreationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/categories/create")
                        .param("name", "CSRF category"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryListUsesServerSidePagination() throws Exception {
        for (int index = 0; index < 25; index++) {
            categories.save(Category.builder().name("Pagination category " + index).build());
        }

        MvcResult firstPageResult = mockMvc.perform(get("/admin/categories").param("page", "0"))
                .andExpect(status().isOk())
                .andReturn();
        org.springframework.data.domain.Page<?> firstPage =
                (org.springframework.data.domain.Page<?>) firstPageResult.getModelAndView().getModel().get("categoriesPage");
        assertEquals(20, firstPage.getNumberOfElements());
        assertTrue(firstPage.getTotalElements() >= 25);
        assertTrue(firstPageResult.getResponse().getContentAsString().contains("Page 1 of"));

        MvcResult secondPageResult = mockMvc.perform(get("/admin/categories").param("page", "1"))
                .andExpect(status().isOk())
                .andReturn();
        org.springframework.data.domain.Page<?> secondPage =
                (org.springframework.data.domain.Page<?>) secondPageResult.getModelAndView().getModel().get("categoriesPage");
        assertTrue(secondPage.getNumberOfElements() > 0);
        assertTrue(secondPage.getNumber() > firstPage.getNumber());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void categoryOptionsAreSearchableAndReturnedInPages() throws Exception {
        for (int index = 0; index < 13; index++) {
            categories.save(Category.builder().name("PickerSearch " + index).build());
        }

        mockMvc.perform(get("/admin/categories/options")
                        .param("query", "PickerSearch")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(12))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/admin/categories/options")
                        .param("query", "PickerSearch")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void categoryOptionsIncludeChildrenAndDescendantEndpointReturnsNestedCategories() throws Exception {
        Category horror = categories.save(Category.builder().name("CascadeTest Horror").build());
        Category slasher = categories.save(Category.builder().name("CascadeTest Slasher").parent(horror).build());
        categories.save(Category.builder().name("CascadeTest Teen Slasher").parent(slasher).build());
        categories.save(Category.builder().name("CascadeTest Paranormal").parent(horror).build());

        mockMvc.perform(get("/admin/categories/options").param("query", "CascadeTest Horror"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].hasChildren").value(true));

        mockMvc.perform(get("/admin/categories/{id}/descendants", horror.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.name == 'CascadeTest Teen Slasher')]").exists());
    }
}
