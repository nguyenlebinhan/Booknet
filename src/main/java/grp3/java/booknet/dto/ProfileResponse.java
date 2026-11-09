package grp3.java.booknet.dto;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;

public record ProfileResponse(Long id, String email, String fullName, String username,
                              String phone, String address, String avatarUrl, String memberCode,
                              UserRole role, AuthProvider provider, boolean emailVerified, boolean googleLinked) {
    public static ProfileResponse from(User user, boolean googleLinked) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getUsername(),
                user.getPhone(), user.getAddress(), user.getAvatarUrl(), user.getMemberCode(),
                user.getRole(), user.getProvider(), user.isEmailVerified(), googleLinked);
    }
}
