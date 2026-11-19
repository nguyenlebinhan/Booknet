package grp3.java.booknet.service.author;

public class InvalidAuthorImageException extends RuntimeException {

    public InvalidAuthorImageException(String message, Throwable cause) {
        super(message
                .replace("cover images", "author photos")
                .replace("cover image", "author photo")
                .replace("Cover image", "Author photo"), cause);
    }
}
