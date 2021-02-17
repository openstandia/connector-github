package jp.openstandia.connector.github;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamAssignmentResolverTest {

    Set<String> addTeams;
    Set<String> removeTeams;
    Set<String> addMaintainerTeams;
    Set<String> removeMaintainerTeams;

    @BeforeEach
    void init() {
        addTeams = new HashSet<>();
        removeTeams = new HashSet<>();
        addMaintainerTeams = new HashSet<>();
        removeMaintainerTeams = new HashSet<>();
    }

    @Test
    void addTeamOnly() {
        addTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddTeams.size());
        assertTrue(resolver.resolvedAddTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddMaitainerTeams.size());
        assertEquals(0, resolver.resolvedRemoveTeams.size());
    }

    @Test
    void addMaintainerTeamOnly() {
        addMaintainerTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddMaitainerTeams.size());
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddTeams.size());
        assertEquals(0, resolver.resolvedRemoveTeams.size());
    }

    @Test
    void removeTeamOnly() {
        removeTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedRemoveTeams.size());
        assertTrue(resolver.resolvedRemoveTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddTeams.size());
        assertEquals(0, resolver.resolvedAddMaitainerTeams.size());
    }

    @Test
    void addTeamConflict() {
        addTeams.add("t1");
        addMaintainerTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddMaitainerTeams.size());
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddTeams.size(), "Should be deleted");
        assertEquals(0, resolver.resolvedRemoveTeams.size());
    }

    @Test
    void removeTeamConflict() {
        removeTeams.add("t1");
        removeMaintainerTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedRemoveTeams.size(), "Should be deleted the duplication");
        assertTrue(resolver.resolvedRemoveTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddTeams.size());
        assertEquals(0, resolver.resolvedAddMaitainerTeams.size());
    }

    @Test
    void addTeamAndMaintainerTeam() {
        addTeams.add("t1");
        addMaintainerTeams.add("t2");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddTeams.size());
        assertTrue(resolver.resolvedAddTeams.contains("t1"));

        assertEquals(1, resolver.resolvedAddMaitainerTeams.size());
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t2"));

        assertEquals(0, resolver.resolvedRemoveTeams.size());
    }

    @Test
    void removeTeamAndMaintainerTeam() {
        removeTeams.add("t1");
        removeMaintainerTeams.add("t2");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(2, resolver.resolvedRemoveTeams.size());
        assertTrue(resolver.resolvedRemoveTeams.contains("t1"));
        assertTrue(resolver.resolvedRemoveTeams.contains("t2"));

        assertEquals(0, resolver.resolvedAddTeams.size());
        assertEquals(0, resolver.resolvedAddMaitainerTeams.size());
    }

    @Test
    void switchTeamToMaintainerTeam() {
        addMaintainerTeams.add("t1");
        removeTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddMaitainerTeams.size());
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddTeams.size());
        assertEquals(0, resolver.resolvedRemoveTeams.size(), "Should be deleted for switching");
    }

    @Test
    void switchMaintainerTeamToTeam() {
        addTeams.add("t1");
        removeMaintainerTeams.add("t1");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(1, resolver.resolvedAddTeams.size());
        assertTrue(resolver.resolvedAddTeams.contains("t1"));

        assertEquals(0, resolver.resolvedAddMaitainerTeams.size());
        assertEquals(0, resolver.resolvedRemoveTeams.size(), "Should be deleted for switching");
    }

    @Test
    void complex() {
        addTeams.add("t1");
        addTeams.add("t2");
        addTeams.add("t3");
        removeTeams.add("t4");
        removeTeams.add("t5");
        removeTeams.add("t6");
        addMaintainerTeams.add("t1");
        addMaintainerTeams.add("t4");
        addMaintainerTeams.add("t7");
        removeMaintainerTeams.add("t2");
        removeMaintainerTeams.add("t5");
        removeMaintainerTeams.add("t8");

        TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

        assertEquals(2, resolver.resolvedAddTeams.size());
        assertTrue(resolver.resolvedAddTeams.contains("t2"));
        assertTrue(resolver.resolvedAddTeams.contains("t3"));

        assertEquals(3, resolver.resolvedAddMaitainerTeams.size());
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t1"));
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t4"));
        assertTrue(resolver.resolvedAddMaitainerTeams.contains("t7"));

        assertEquals(3, resolver.resolvedRemoveTeams.size());
        assertTrue(resolver.resolvedRemoveTeams.contains("t5"));
        assertTrue(resolver.resolvedRemoveTeams.contains("t6"));
        assertTrue(resolver.resolvedRemoveTeams.contains("t8"));
    }
}