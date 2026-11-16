package grp3.java.booknet.security.service;

import grp3.java.booknet.security.CustomOAuth2User;
import grp3.java.booknet.service.impl.OAuthAccountServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuthAccountServiceImpl accounts;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User google = super.loadUser(request);
        return new CustomOAuth2User(google, accounts.authenticate(google));
    }
}
