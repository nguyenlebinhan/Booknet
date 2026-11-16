package grp3.java.booknet.service;

import grp3.java.booknet.entity.user.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuthAccountService {
    public User authenticate(OAuth2User oAuth2User);

}
