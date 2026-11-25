package grp3.java.booknet.service.assistant;

import java.io.Serializable;
import java.util.List;

/**
 * One question and its answer, kept only in the reader's HTTP session.
 *
 * @param answered   true when Gemini gave an on-topic answer; only these turns are sent back as context
 * @param searchLink true when the page should also offer a catalog search for the question
 */
public record AssistantTurn(String question, String answer, List<Long> bookIds, boolean answered, boolean searchLink)
        implements Serializable {
}
