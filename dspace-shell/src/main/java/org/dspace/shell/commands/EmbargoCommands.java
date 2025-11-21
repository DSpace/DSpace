/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Embargo related commands
 * @author paulo-graca
 *
 */
@Command(
    command = "embargo",
    group = "Embargo commands"
)
public class EmbargoCommands {

    private static final Logger log = LoggerFactory.getLogger(EmbargoCommands.class);

    /**
     * Discovery commands to use for indexing
     */
    private final DiscoveryCommands discoveryCommands;

    public EmbargoCommands (DiscoveryCommands discoveryCommands) {
        this.discoveryCommands = discoveryCommands;
    }

    /**
     * Release embargo
     * It will process Items or Bitstreams that released embargo and index them
     * @param nDays num
     * @throws Exception
     */
    @Command(
        command = "lifter",
        alias = "embargo-lifter",
        description = "this command executes embargo lifter by reindexing content"
    )
    public void embargoLifter(
        @Option(
            longNames = "nDays",
            description =
                "The number of past days to process embargo. As an example, "
                + "nDays=5 means process the past 5 days.",
            defaultValue = "1"
        )
        int nDays
    ) throws Exception {

        //TODO: Create DSpace API services for getting DSpaceObjects that
        //expired recently (today - nDays)
        //discoveryCommands.indexDiscovery(
        //    false,
        //    false,
        //    true,
        //    "d2391854-aaf6-4782-b455-5eb1aba5cbf4",
        //    null,
        //    false);
        System.out.println(
            "Indexed"
        );
        System.out.println("number of days " + nDays);
    }

}
