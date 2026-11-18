package grp3.java.booknet.controller.author;

import grp3.java.booknet.dto.author.AuthorOptionDto;
import grp3.java.booknet.dto.author.AuthorOptionsPageDto;
import grp3.java.booknet.service.author.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StaffAuthorOptionsController {

    private static final int PAGE_SIZE = 12;

    private final AuthorService authorService;

    @GetMapping("/api/staff/authors/options")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public AuthorOptionsPageDto authorOptions(@RequestParam(defaultValue = "") String query,
                                              @RequestParam(defaultValue = "0") int page) {
        Page<AuthorOptionDto> authors = authorService.listAuthors(
                        query,
                        PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by("name").ascending()))
                .map(AuthorOptionDto::from);
        return new AuthorOptionsPageDto(
                authors.getContent(),
                authors.getNumber(),
                authors.getTotalPages(),
                authors.getTotalElements());
    }
}
