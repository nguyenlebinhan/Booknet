package grp3.java.booknet.security;

import grp3.java.booknet.entity.user.User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;

public class CustomOidcUser extends CustomOAuth2User implements OidcUser {
    private final OidcUser delegate;

    public CustomOidcUser(OidcUser delegate, User user) {
        super(delegate, user);
        this.delegate = delegate;
    }

    @Override public Map<String, Object> getClaims() { return delegate.getClaims(); }
    @Override public OidcUserInfo getUserInfo() { return delegate.getUserInfo(); }
    @Override public OidcIdToken getIdToken() { return delegate.getIdToken(); }
}
