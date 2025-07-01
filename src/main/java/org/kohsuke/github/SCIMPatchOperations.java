package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SCIMPatchOperations {
    private static final String PATCH_OP = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    @JsonProperty("schemas")
    public List<String> schemas = Collections.singletonList(PATCH_OP);

    @JsonProperty("Operations")
    public List<Operation> operations = new ArrayList<>();

    public void replace(String path, String value) {
        Operation op = new Operation();
        op.op = "replace";
        op.path = path;
        op.value = value == null ? "" : value;
        operations.add(op);
    }

    public void replace(String path, Boolean value) {
        Operation op = new Operation();
        op.op = "replace";
        op.path = path;
        op.value = value;
        operations.add(op);
    }

    public void replace(SCIMEmail value) {
        if (value == null) {
            operations.add(removeAllOp("emails"));
            return;
        }

        List<SCIMEmail> emails = new ArrayList<>();
        emails.add(value);

        Operation op = new Operation();
        op.op = "replace";
        op.path = "emails";
        op.value = emails;
        operations.add(op);
    }

    public void replace(SCIMRole value) {
        if (value == null) {
            operations.add(removeAllOp("roles"));
            return;
        }

        List<SCIMRole> roles = new ArrayList<>();
        roles.add(value);

        Operation op = new Operation();
        op.op = "replace";
        op.path = "roles";
        op.value = roles;
        operations.add(op);
    }

    private Operation removeAllOp(String path) {
        // GitHub SCIM API workaround for removing optional multi-valued attributes:
        // Use [{"value": ""}] to remove roles attribute.
        //
        // Standard approaches that don't work:
        //
        // 1. Empty array
        //    {"op": "replace", "path": "roles", "value": []}
        //    → Error 400: {"scimType": "invalidSyntax",
        //                  "detail": "Misconfigured IdP, missing or misconfigured attributes:\n
        //                            No subschema in \"oneOf\" matched.\n
        //                            For 'oneOf/0', [] is not a string.\n
        //                            For 'oneOf/1', [] is not a boolean.\n
        //                            For 'oneOf/2', [] is not an object.\n
        //                            1 item required; only 0 were supplied."}
        //
        // 2. Remove operation:
        //    {"op": "remove", "path": "roles"}
        //    → HTTP 200 OK (success response) but roles attribute is NOT removed
        //
        // Workaround results:
        // - roles (optional): [{"value": ""}] → Success, attribute removed
        // - emails (required): [{"value": ""}] → Error 400:
        //                      {"scimType": "invalidSyntax",
        //                       "detail": "Email values must be valid email addresses."}
        Operation op = new Operation();
        op.op = "replace";
        op.path = path;

        List<Map<String, String>> values = new ArrayList<>();
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("value", "");
        values.add(valueMap);

        op.value = values;
        return op;
    }

    public void addMembers(List<String> values) {
        List<Member> members = values.stream().map(v -> {
            Member member = new Member();
            member.value = v;
            return member;
        }).collect(Collectors.toList());

        Operation op = new Operation();
        op.op = "add";
        op.path = "members";
        op.value = members;

        operations.add(op);
    }

    public void removeMembers(List<String> values) {
        List<Member> members = values.stream().map(v -> {
            Member member = new Member();
            member.value = v;
            return member;
        }).collect(Collectors.toList());

        Operation op = new Operation();
        op.op = "remove";
        op.path = "members";
        op.value = members;

        operations.add(op);
    }

    public static class Operation {
        public String op;
        public String path;
        public Object value;
    }

    public static class Member {
        public String value;
    }

    public boolean hasAttributesChange() {
        return !operations.isEmpty();
    }
}