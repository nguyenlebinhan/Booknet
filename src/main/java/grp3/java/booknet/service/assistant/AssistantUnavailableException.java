package grp3.java.booknet.service.assistant;

/** Gemini could not answer: no key, timeout, quota used up, blocked or unreadable response. */
public class AssistantUnavailableException extends RuntimeException {
    public AssistantUnavailableException(String message) {
        super(message);
    }
}
