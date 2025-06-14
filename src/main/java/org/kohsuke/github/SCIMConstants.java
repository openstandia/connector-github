package org.kohsuke.github;

/**
 * Constants for SCIM API operations.
 *
 * @author Hiroyuki Wada
 */
public final class SCIMConstants {

    public static final String SCIM_CONTENT_TYPE = "application/scim+json";
    public static final String SCIM_ACCEPT = "application/scim+json";
    public static final String GITHUB_API_VERSION = "2022-11-28";
    
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_API_VERSION = "X-GitHub-Api-Version";

    public static final String SCIM_USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String SCIM_GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
}