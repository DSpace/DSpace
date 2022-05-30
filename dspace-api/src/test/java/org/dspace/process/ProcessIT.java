/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.process;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.junit.Test;

/**
 * This class will aim to test Process related use cases
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ProcessIT extends AbstractIntegrationTestWithDatabase {

    protected ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Test
    public void checkProcessGroupsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupA = GroupBuilder.createGroup(context)
            .withName("Group A")
            .addMember(admin)
            .build();

        Set<Group> groupSet = new HashSet<>();
        groupSet.add(groupA);

        Process processA = ProcessBuilder.createProcess(context, admin, "mock-script",
                                                        new LinkedList<>(),
                                                        groupSet).build();

        context.restoreAuthSystemState();
        Process process = processService.find(context, processA.getID());
        List<Group> groups = process.getGroups();
        boolean isPresent = groups.stream().anyMatch(g -> g.getID().equals(groupA.getID()));
        assertTrue(isPresent);
    }

    @Test
    public void removeOneGroupTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Group groupA = GroupBuilder.createGroup(context)
            .withName("Group A")
            .addMember(admin).build();

        Set<Group> groupSet = new HashSet<>();
        groupSet.add(groupA);

        UUID groupUuid = groupA.getID();
        Process processA = ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>(),
                                                        groupSet).build();

        context.restoreAuthSystemState();

        groupService.delete(context, groupA);
        context.commit();
        context.reloadEntity(groupA);
        processA = context.reloadEntity(processA);

        Process process = processService.find(context, processA.getID());
        List<Group> groups = process.getGroups();
        boolean isPresent = groups.stream().anyMatch(g -> g.getID().equals(groupUuid));
        assertFalse(isPresent);

    }
}
