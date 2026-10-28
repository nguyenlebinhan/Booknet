package grp3.java.booknet.security;

public final class OAuthLinkSessionAttributes {

    public static final String GOOGLE_LINK_USER_ID = "BOOKNET_GOOGLE_LINK_USER_ID";
    public static final String GOOGLE_LINK_STARTED_AT = "BOOKNET_GOOGLE_LINK_STARTED_AT";
    public static final String GOOGLE_LINK_SUCCEEDED = "BOOKNET_GOOGLE_LINK_SUCCEEDED";
    public static final long GOOGLE_LINK_MAX_AGE_MILLIS = 10 * 60 * 1000L;

    private OAuthLinkSessionAttributes() {
    }
}
