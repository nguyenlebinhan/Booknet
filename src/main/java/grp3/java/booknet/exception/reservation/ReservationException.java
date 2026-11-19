package grp3.java.booknet.exception.reservation;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReservationException extends RuntimeException {

    private final HttpStatus status;

    public ReservationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ReservationException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }
}
