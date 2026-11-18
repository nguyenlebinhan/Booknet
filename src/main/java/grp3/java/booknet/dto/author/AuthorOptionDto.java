package grp3.java.booknet.dto.author;

import grp3.java.booknet.entity.book.Author;

public record AuthorOptionDto(Integer id, String name) {

    public static AuthorOptionDto from(Author author) {
        return new AuthorOptionDto(author.getId(), author.getName());
    }
}
