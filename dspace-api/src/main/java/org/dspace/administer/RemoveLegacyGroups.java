/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * remove-legacy-groups
 */
public final class RemoveLegacyGroups {
    /**
     * DSpace Context object
     */
    private final Context context;

    protected GroupService groupService;

    /**
     * For invoking via the command line.
     *
     * @param argv command-line arguments
     * @throws Exception if error
     */
    public static void main(String[] argv)
            throws Exception {

        RemoveLegacyGroups removeLegacyGroups = new RemoveLegacyGroups();

        removeLegacyGroups.perform();

    }

    /**
     * constructor, which just creates an object with a ready context
     *
     * @throws Exception if error
     */
    protected RemoveLegacyGroups()
            throws Exception {
        context = new Context();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
    }

    /**
     * Remove legacy groups
     */
    protected void perform()
            throws Exception {

        context.turnOffAuthorisationSystem();

        List<Group> groupsToRemove = new ArrayList<>();

        List<MetadataField> sortFields = new ArrayList<>();
        List<Group> groups = groupService.findAll(context, sortFields);

        String pattern = "COMMUNITY_\\d{1,}_MEMBER";

        for (Group group : groups) {
            if(group.getName().equals("COMMUNITY_WHEEL")) {
                groupsToRemove.add(group);
            } else if (Pattern.matches(pattern, group.getName())) {
                groupsToRemove.add(group);
            }
        }

        if (groupsToRemove.isEmpty()) {
            System.out.println("No legacy groups found");
        } else {

            // Remove the groups
            for (Group groupToRemove : groupsToRemove) {
                System.out.println("Removing group '" + groupToRemove.getName() + "'");
                groupService.delete(context, groupToRemove);
            }

            System.out.println("Legacy groups removed");
        }

        context.complete();

    }
}
