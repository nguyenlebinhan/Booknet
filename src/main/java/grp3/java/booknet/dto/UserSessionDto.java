package grp3.java.booknet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fullName;
    private String email;
    private String memberCode;
    private String roleName;

    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "BN";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
