package grp3.java.booknet.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceForm {
    @Builder.Default
    private boolean websiteEnabled = true;
    @Builder.Default
    private boolean emailEnabled = true;
    @Builder.Default
    private boolean eventsNewsletterEnabled = false;
}
