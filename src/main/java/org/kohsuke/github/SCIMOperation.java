package org.kohsuke.github;

/**
 * Represents the result of a SCIM operation.
 *
 * @author Hiroyuki Wada
 */
public class SCIMOperation<T> {
    public final String op;
    public final String path;
    public final T value;

    public SCIMOperation(String op, String path, T value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }
}
