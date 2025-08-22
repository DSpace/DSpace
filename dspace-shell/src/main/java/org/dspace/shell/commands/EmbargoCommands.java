/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class EmbargoCommands {
    
    @ShellMethod(key="embargo-lifter", value="this command executes embargo lifter by reindexing content")
    public String embargoLifter(@ShellOption(value="nDays", defaultValue=1) int nDays) throws Exception {

        return "number of days " + Integer.toString(nDays);
    }

}
