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
import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * remove-group -n [group name]
 */
public final class RemoveGroup {
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

        String usage = "Usage: remove-group -n <group name>";

        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("n", "name", true, "group name");

        CommandLine line = parser.parse(options, argv);

        RemoveGroup removeGroup = new RemoveGroup();

        String value = "";

        if(line.hasOption("n")) {
            value = line.getOptionValue("n");
        }

        if(StringUtils.isNotEmpty(value)) {
            removeGroup.perform(value);
        } else {
            System.out.println(usage);
            System.exit(0);
        }
    }

    /**
     * constructor, which just creates an object with a ready context
     *
     * @throws Exception if error
     */
    protected RemoveGroup()
            throws Exception {
        context = new Context();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
    }

    /**
     * Remove group with the given name.
     */
    protected void perform(String name)
            throws Exception {

        context.turnOffAuthorisationSystem();

        // Find group
        Group groupToRemove = groupService.findByName(context, name);

        if (groupToRemove == null) {
            throw new IllegalStateException("Error, no group found with name '" + name + "'");
        }

        // Remove the group
        groupService.delete(context, groupToRemove);

        context.complete();

        System.out.println("Group with name '" + name + "' removed");
    }
}
