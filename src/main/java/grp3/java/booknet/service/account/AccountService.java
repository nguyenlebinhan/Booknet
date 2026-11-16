package grp3.java.booknet.service.account;

import grp3.java.booknet.dto.user.response.ProfileResponse;
import grp3.java.booknet.dto.user.request.ChangePasswordRequest;
import grp3.java.booknet.dto.user.request.ProfileUpdateRequest;
import grp3.java.booknet.dto.user.request.ResetPasswordRequest;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AccountTokenPurpose;

public interface AccountService {
    public void sendInitialVerification(User user);
    public void requestEmail(String email, AccountTokenPurpose purpose);
    public void requestPasswordSetup(Long userId);
    public void verifyEmail(String rawToken);
    public void validateResetToken(String rawToken);
    public void resetPassword(ResetPasswordRequest request);
    public void changePassword(Long userId, ChangePasswordRequest request);
    public ProfileResponse profile(Long userId);
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);

}
