/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

public class EmbargoCommands {

    @Command(command = "embargo-lifter", description="this command executes embargo lifter by reindexing content")
    public String embargoLifter(@Option(longNames="nDays", defaultValue="1") int nDays) throws Exception {
        return "number of days " + nDays;
    }

}
