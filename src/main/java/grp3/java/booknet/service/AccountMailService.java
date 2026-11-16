package grp3.java.booknet.service;

import grp3.java.booknet.enums.user.AccountTokenPurpose;

public interface AccountMailService {
    public void send(String email, String token, AccountTokenPurpose purpose);
}
