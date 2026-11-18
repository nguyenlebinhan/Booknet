package grp3.java.booknet.dto.catalog;

import grp3.java.booknet.enums.book.PublicationStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Read-only data for the public Book details page. Never bound from a request. */
public record BookDetailsView(
        Long id,
        String bookCode,
        String title,
        List<String> authors,
        String translator,
        List<String> categories,
        String description,
        List<String> keywords,
        String coverUrl,
        String publisher,
        Short publishYear,
        String edition,
        Integer pageCount,
        String language,
        String isbn,
        String shelfLocation,
        PublicationStatus publicationStatus,
        long totalCopies,
        long availableCopies,
        long onLoanCopies,
        long otherCopies,
        List<String> availableBarcodes,
        List<CopyRow> copies,
        LocalDate earliestReturn,
        boolean popular,
        LocalDate loanDate,
        LocalDate dueDate,
        List<CatalogBookView> related
) {
    /** One physical copy: its barcode, what it is doing now, and where it lives. */
    public record CopyRow(String barcode, String status, String tone, String shelf) { }

    public static final int LOAN_DAYS = 14;
    public static final int MAX_BOOKS = 5;
    public static final int RENEWAL_DAYS = 7;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public boolean published() { return publicationStatus == PublicationStatus.PUBLISHED; }
    public boolean onShelf() { return published() && availableCopies > 0; }
    /** Every copy is out, so a member could wait for one. */
    public boolean allOnLoan() { return published() && availableCopies == 0 && onLoanCopies > 0; }

    public String authorLine() { return authors.isEmpty() ? "Unknown author" : String.join(", ", authors); }
    public String genre() { return categories.isEmpty() ? null : categories.getFirst(); }
    public String kicker() { return genre() == null ? "THE BOOKNET COLLECTION" : genre().toUpperCase(java.util.Locale.ROOT); }

    /** First sentence as the large lead; the rest as body text. */
    public String lead() {
        if (description == null || description.isBlank()) return null;
        int end = description.indexOf(". ");
        return end < 0 ? description.strip() : description.substring(0, end + 1).strip();
    }

    public String bodyText() {
        if (description == null || description.isBlank()) return null;
        int end = description.indexOf(". ");
        return end < 0 ? null : description.substring(end + 2).strip();
    }

    public String pagesAndLanguage() {
        String pages = pageCount == null ? null : pageCount + " pages";
        if (pages == null) return language;
        return language == null ? pages : pages + " / " + language;
    }

    public String editionLine() {
        StringBuilder line = new StringBuilder();
        if (publisher != null) line.append(publisher);
        if (edition != null && !edition.isBlank()) line.append(line.isEmpty() ? "" : " · ").append(edition);
        if (publishYear != null) line.append(line.isEmpty() ? "" : " · ").append(publishYear);
        return line.isEmpty() ? null : line.toString();
    }

    public String copyBadge() {
        if (!published()) return publicationStatus == PublicationStatus.DRAFT ? "Draft" : "Discontinued";
        if (availableCopies > 0) return "Available · " + availableCopies + "/" + totalCopies + " copies";
        if (onLoanCopies > 0) return "On loan";
        return "Not in circulation";
    }

    public String loanDateText() { return DATE.format(loanDate); }
    public String dueDateText() { return DATE.format(dueDate); }
    public String earliestReturnText() { return earliestReturn == null ? null : DATE.format(earliestReturn); }
}
