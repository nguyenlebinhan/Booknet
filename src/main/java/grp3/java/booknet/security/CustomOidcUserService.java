package grp3.java.booknet.security;

import grp3.java.booknet.service.OAuthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final OAuthAccountService accounts;

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser google = super.loadUser(request);
        return new CustomOidcUser(google, accounts.authenticate(google));
    }
}
