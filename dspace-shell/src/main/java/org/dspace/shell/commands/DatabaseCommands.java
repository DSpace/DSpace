/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.springframework.shell.command.annotation.Command;

/**
 * Database commands for the DSpace Spring Shell
 */
@Command(command = "database")
public class DatabaseCommands {

    @Command(command = "info", description="Displays database information using the DSpace context")
    public void dbInfo() {
        System.out.println("hello!");
    }
}
