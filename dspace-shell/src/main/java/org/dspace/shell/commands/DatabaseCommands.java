/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
//import org.springframework.shell.standard.ShellOption;

/**
 * Database commands for the DSpace Spring Shell
 */
@ShellComponent
@ShellCommandGroup("database")
public class DatabaseCommands {

    @ShellMethod(key="info", value="Displays database information using the DSpace context")
    public void dbInfo() {
        System.out.println("hello!");
    }
}
