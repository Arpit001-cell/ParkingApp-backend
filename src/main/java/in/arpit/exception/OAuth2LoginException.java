package in.arpit.exception;

/** Thrown while finishing a Google login. The message is a short, safe, machine-readable code. */
public class OAuth2LoginException extends RuntimeException {
    public OAuth2LoginException(String code) {
        super(code);
    }
}
