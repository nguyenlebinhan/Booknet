package grp3.java.booknet.dto.book.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BookDeletionCheckDto {
    Long bookId;
    String bookCode;
    String title;
    boolean hardDeleteAllowed;
    List<String> blockingReferences;
}
