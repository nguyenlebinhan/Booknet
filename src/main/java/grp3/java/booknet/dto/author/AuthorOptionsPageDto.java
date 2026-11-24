package grp3.java.booknet.dto.author;

import java.util.List;

public record AuthorOptionsPageDto(
        List<AuthorOptionDto> authors,
        int page,
        int totalPages,
        long totalElements) {
}
