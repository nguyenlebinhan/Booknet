package grp3.java.booknet.security.session;

import grp3.java.booknet.repository.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/** Role/access/password changes invalidate cookies, even after a subsequent unlock. */
public class VersionedRememberMeServices extends TokenBasedRememberMeServices {
    private final UserRepository users;
    public VersionedRememberMeServices(String key, UserDetailsService details, UserRepository users) {
        super(key, details);
        this.users = users;
        setTokenValiditySeconds(30 * 24 * 60 * 60);
    }
    @Override
    protected String makeTokenSignature(long expires, String username, String password, RememberMeTokenAlgorithm algorithm) {
        String revision = users.findByEmail(username).map(u -> Long.toString(u.getAuthVersion())).orElse("missing");
        return super.makeTokenSignature(expires, username, password + ":" + revision, algorithm);
    }
}
