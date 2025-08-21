/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseInfo;
import org.springframework.shell.command.annotation.Command;

/**
 * Database commands for the DSpace Spring Shell
 */
@Command(group = "Database", command = "db-info")
public class DatabaseCommands {

    @Command(description = "Displays database information using the DSpace context")
    public void dbInfo() {
        try (Context context = new Context()) {
            DatabaseInfo.printInfo(context);
        } catch (Exception e) {
            System.err.println("Error while retrieving database information: " + e.getMessage());
        }
    }
}
