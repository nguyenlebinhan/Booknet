package grp3.java.booknet.dto.member;
import jakarta.validation.constraints.*;
public record AdminControlForm(@NotBlank @Size(max=300) String reason,@NotNull Long revision,@NotNull Boolean confirmed) {}
