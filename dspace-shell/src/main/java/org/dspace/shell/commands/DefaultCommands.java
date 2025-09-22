/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.command.CommandAlias;
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
        System.out.println("# DSpace Shell - command list");
        System.out.println("");
        for (Map.Entry<String, CommandRegistration> entry : commandCatalog.getRegistrations().entrySet()) {
            String keyName = entry.getKey();
            CommandRegistration registration = entry.getValue();
            String commandName = registration.getCommand();

            // If the iterator is the alias, skip it
            if (!keyName.equals(commandName)) {
                continue;
            }
            String aliases = registration.getAliases().stream()
                    .map(CommandAlias::getCommand)
                    .collect(Collectors.joining(", "));

            System.out.println("## " + commandName );
            System.out.println("");

            System.out.println("**Group:** `" + registration.getGroup() + "`" );
            System.out.println("");

            if (aliases.isEmpty()) {
                System.out.println("Alias: `" + aliases + "`" );
                System.out.println("");
            }

            System.out.println(registration.getDescription());
            System.out.println("");
            System.out.println("usage: `"+ commandName + " [OPTIONS]`");
            System.out.println("");
            System.out.println("| Option | Description | Type | Required |");
            System.out.println("| ------ | ------ | ------ | ------ |");

            registration.getOptions().forEach(option -> {
                System.out.print("| --" + String.join(", --", option.getLongNames()));
                System.out.print(" | " + option.getDescription());
                System.out.print(" | " + option.getType().getType().getTypeName());
                System.out.print(" | " + option.isRequired());
                System.out.println(" |");
            });

            System.out.println("");
        }
    }
}
