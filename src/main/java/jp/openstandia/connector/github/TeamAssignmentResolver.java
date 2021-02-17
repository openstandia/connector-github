/*
 *  Copyright Nomura Research Institute, Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jp.openstandia.connector.github;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamAssignmentResolver {
    private final Set<String> origAddTeams;
    private final Set<String> origRemoveTeams;
    private final Set<String> origAddMaintainerTeams;
    private final Set<String> origRemoveMaintainerTeams;

    public Set<String> resolvedAddTeams;
    public Set<String> resolvedAddMaitainerTeams;
    public Set<String> resolvedRemoveTeams;

    public TeamAssignmentResolver(Set<String> addTeams, Set<String> removeTeams, Set<String> addMaintainerTeams, Set<String> removeMaintainerTeams) {
        this.origAddTeams = Collections.unmodifiableSet(addTeams);
        this.origRemoveTeams = Collections.unmodifiableSet(removeTeams);
        this.origAddMaintainerTeams = Collections.unmodifiableSet(addMaintainerTeams);
        this.origRemoveMaintainerTeams = Collections.unmodifiableSet(removeMaintainerTeams);

        resolve();
    }

    private void resolve() {
        // If same team is assigned for both teams and maintainer teams, we assign as maintainer.
        Set<String> addTeams = origAddTeams.stream()
                .filter(t -> !origAddMaintainerTeams.contains(t))
                .collect(Collectors.toSet());

        this.resolvedAddTeams = Collections.unmodifiableSet(addTeams);

        // if same team is unassigned as member and assigned as maintainer, we assign as maintainer.
        Set<String> removeTeams = origRemoveTeams.stream()
                .filter(t -> !origAddMaintainerTeams.contains(t))
                .collect(Collectors.toSet());

        // if same team is unassigned as maintainer and assigned as member, we assign as member.
        Set<String> removeMaintainerTeams = origRemoveMaintainerTeams.stream()
                .filter(t -> !addTeams.contains(t))
                .collect(Collectors.toSet());

        this.resolvedAddMaitainerTeams = origAddMaintainerTeams;

        // If same team is unassigned for both teams and maintainer teams, we only unassign it one time.
        this.resolvedRemoveTeams = Collections.unmodifiableSet(
                Stream.concat(removeTeams.stream(), removeMaintainerTeams.stream()).collect(Collectors.toSet())
        );
    }
}