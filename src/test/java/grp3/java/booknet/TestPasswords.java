package grp3.java.booknet;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Ephemeral credentials for isolated H2 test users; never used by the application. */
final class TestPasswords {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TestPasswords() { }

    static String valid() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return "Aa1" + HexFormat.of().formatHex(bytes);
    }
}
