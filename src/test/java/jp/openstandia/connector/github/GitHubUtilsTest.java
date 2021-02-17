package jp.openstandia.connector.github;

import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubUtilsTest {

    @Test
    void getUserLogin() {
        assertEquals("", GitHubUtils.getUserLogin(":foo@example.com"));
        assertEquals("foo", GitHubUtils.getUserLogin("foo:foo@example.com"));
        assertThrows(InvalidAttributeValueException.class, () -> GitHubUtils.getUserLogin("foo"));
    }
}