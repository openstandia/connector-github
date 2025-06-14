package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        Operation op = new Operation();
        op.op = "replace";
        op.path = path;
        op.value = Collections.emptyList();
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