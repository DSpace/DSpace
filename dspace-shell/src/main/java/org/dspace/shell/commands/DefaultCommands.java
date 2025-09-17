/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;

/**
 * Other and default commands
 * @author paulo-graca
 *
 */
@Command
public class DefaultCommands {
    /**
     * command catalog provider
     */
    private final ObjectProvider<CommandCatalog> catalogProvider;

    public DefaultCommands(ObjectProvider<CommandCatalog> catalogProvider) {
        this.catalogProvider = catalogProvider;
    }

    /**
     * Lists the available commands and options
     * This method isn't required at all - we can use -h option
     * it main purpose is to use the inspector
     */
    @Command(
        command = "metadata",
        description = "List all available commands and their options - for test only - you should"
                      + " use -h option or the help command"
    )
    public void listCommands() {
        CommandCatalog commandCatalog = catalogProvider.getObject(); // lazy resolve
        for (Map.Entry<String, CommandRegistration> entry : commandCatalog.getRegistrations().entrySet()) {
            String commandName = entry.getKey();
            CommandRegistration registration = entry.getValue();

            System.out.println("Command: " + commandName);
            System.out.println("  Description: " + registration.getDescription());

            registration.getOptions().forEach(option -> {
                System.out.println("    Option: --" + String.join(", --", option.getLongNames()));
                System.out.println("      Description: " + option.getDescription());
                System.out.println("      Required: " + option.isRequired());
                System.out.println("      Type: " + option.getType().getType().getTypeName());
            });

            System.out.println();
        }
    }
}
