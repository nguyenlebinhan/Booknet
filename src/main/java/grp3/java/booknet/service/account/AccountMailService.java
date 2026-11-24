package grp3.java.booknet.service.account;

import grp3.java.booknet.enums.user.AccountTokenPurpose;

public interface AccountMailService {
    public void send(String email, String token, AccountTokenPurpose purpose);
    default void sendInvitation(String email, String token, String personalMessage) { send(email, token, AccountTokenPurpose.INVITE_ACCOUNT); }
}
