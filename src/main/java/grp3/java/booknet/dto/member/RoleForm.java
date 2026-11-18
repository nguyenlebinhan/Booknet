package grp3.java.booknet.dto.member;
import jakarta.validation.constraints.*;
import java.util.Set;
import grp3.java.booknet.enums.user.RoleCapability;
public record RoleForm(@NotBlank @Size(max=80) String name,@Size(max=500) String description,
 Set<RoleCapability> capabilities,@PositiveOrZero long revision) {}
