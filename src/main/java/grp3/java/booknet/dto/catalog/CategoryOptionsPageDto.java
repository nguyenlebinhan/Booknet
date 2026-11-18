package grp3.java.booknet.dto.catalog;

import java.util.List;

public record CategoryOptionsPageDto(
        List<CategoryOptionDto> categories,
        int page,
        int totalPages,
        long totalElements) {
}
