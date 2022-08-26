/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.launcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.scripts.handler.impl.CommandLineDSpaceRunnableHandler;
import org.dspace.scripts.service.ScriptService;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.RequestService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * A DSpace script launcher.
 *
 * @author Stuart Lewis
 * @author Mark Diggory
 */
public class ScriptLauncher {

    private static final Logger log = LogManager.getLogger();

    /**
     * The service manager kernel
     */
    private static transient DSpaceKernelImpl kernelImpl;

    /**
     * Default constructor
     */
    private ScriptLauncher() {
    }

    /**
     * Execute the DSpace script launcher
     *
     * @param args Any parameters required to be passed to the scripts it executes
     * @throws IOException           if IO error
     * @throws FileNotFoundException if file doesn't exist
     */
    public static void main(String[] args)
        throws FileNotFoundException, IOException, IllegalAccessException, InstantiationException {
        // Initialise the service manager kernel
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning()) {
                kernelImpl.start();
            }
        } catch (Exception e) {
            // Failed to start so destroy it and log and throw an exception
            try {
                kernelImpl.destroy();
            } catch (Exception e1) {
                // Nothing to do
            }
            String message = "Failure during kernel init: " + e.getMessage();
            System.err.println(message);
            e.printStackTrace();
            throw new IllegalStateException(message, e);
        }

        // Load up the ScriptLauncher's configuration
        Document commandConfigs = getConfig();

        // Check that there is at least one argument (if not display command options)
        if (args.length < 1) {
            System.err.println("You must provide at least one command argument");
            display(commandConfigs);
            System.exit(1);
        }

        // Look up command in the configuration, and execute.

        CommandLineDSpaceRunnableHandler commandLineDSpaceRunnableHandler = new CommandLineDSpaceRunnableHandler();
        int status = handleScript(args, commandConfigs, commandLineDSpaceRunnableHandler, kernelImpl);

        // Destroy the service kernel if it is still alive
        if (kernelImpl != null) {
            kernelImpl.destroy();
            kernelImpl = null;
        }

        System.exit(status);

    }

    /**
     * This method will take the arguments from a commandline input and it'll find the script that the first argument
     * refers to and it'll execute this script.
     * It can return a 1 or a 0 depending on whether the script failed or passed respectively
     * @param args                  The arguments for the script and the script as first one in the array
     * @param commandConfigs        The Document
     * @param dSpaceRunnableHandler The DSpaceRunnableHandler for this execution
     * @param kernelImpl            The relevant DSpaceKernelImpl
     * @return A 1 or 0 depending on whether the script failed or passed respectively
     */
    public static int handleScript(String[] args, Document commandConfigs,
                                   DSpaceRunnableHandler dSpaceRunnableHandler,
                                   DSpaceKernelImpl kernelImpl) throws InstantiationException, IllegalAccessException {
        int status;
        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);
        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            status = executeScript(args, dSpaceRunnableHandler, script);
        } else {
            status = runOneCommand(commandConfigs, args, kernelImpl);
        }
        return status;
    }

    /**
     * This method will simply execute the script
     * @param args                  The arguments of the script with the script name as first place in the array
     * @param dSpaceRunnableHandler The relevant DSpaceRunnableHandler
     * @param script                The script to be executed
     * @return A 1 or 0 depending on whether the script failed or passed respectively
     */
    private static int executeScript(String[] args, DSpaceRunnableHandler dSpaceRunnableHandler,
                                     DSpaceRunnable script) {
        try {
            script.initialize(args, dSpaceRunnableHandler, null);
            script.run();
            return 0;
        } catch (ParseException e) {
            script.printHelp();
            e.printStackTrace();
            return 1;
        }
    }

    protected static int runOneCommand(Document commandConfigs, String[] args) {
        return runOneCommand(commandConfigs, args, kernelImpl);
    }

    /**
     * Recognize and execute a single command.
     *
     * @param commandConfigs  Document
     * @param args the command line arguments given
     */
    protected static int runOneCommand(Document commandConfigs, String[] args, DSpaceKernelImpl kernelImpl) {
        String request = args[0];
        Element root = commandConfigs.getRootElement();
        List<Element> commands = root.getChildren("command");
        Element command = null;
        for (Element candidate : commands) {
            if (request.equalsIgnoreCase(candidate.getChild("name").getValue())) {
                command = candidate;
                break;
            }
        }

        if (null == command) {
            // The command wasn't found
            System.err.println("Command not found: " + args[0]);
            display(commandConfigs);
            return 1;
        }

        // Run each step
        List<Element> steps = command.getChildren("step");
        for (Element step : steps) {
            // Instantiate the class
            Class target = null;

            // Is it the special case 'dsrun' where the user provides the class name?
            String className;
            if ("dsrun".equals(request)) {
                if (args.length < 2) {
                    System.err.println("Error in launcher.xml: Missing class name");
                    return 1;
                }
                className = args[1];
            } else {
                className = step.getChild("class").getValue();
            }
            try {
                target = Class.forName(className,
                                       true,
                                       Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                System.err.println("Error in launcher.xml: Invalid class name: " + className);
                return 1;
            }

            // Strip the leading argument from the args, and add the arguments
            // Set <passargs>false</passargs> if the arguments should not be passed on
            String[] useargs = args.clone();
            Class[] argTypes = {useargs.getClass()};
            boolean passargs = true;
            if ((step.getAttribute("passuserargs") != null) &&
                ("false".equalsIgnoreCase(step.getAttribute("passuserargs").getValue()))) {
                passargs = false;
            }
            if ((args.length == 1) || (("dsrun".equals(request)) && (args.length == 2)) || (!passargs)) {
                useargs = new String[0];
            } else {
                // The number of arguments to ignore
                // If dsrun is the command, ignore the next, as it is the class name not an arg
                int x = 1;
                if ("dsrun".equals(request)) {
                    x = 2;
                }
                String[] argsnew = new String[useargs.length - x];
                for (int i = x; i < useargs.length; i++) {
                    argsnew[i - x] = useargs[i];
                }
                useargs = argsnew;
            }

            // Add any extra properties
            List<Element> bits = step.getChildren("argument");
            if (step.getChild("argument") != null) {
                String[] argsnew = new String[useargs.length + bits.size()];
                int i = 0;
                for (Element arg : bits) {
                    argsnew[i++] = arg.getValue();
                }
                for (; i < bits.size() + useargs.length; i++) {
                    argsnew[i] = useargs[i - bits.size()];
                }
                useargs = argsnew;
            }

            // Establish the request service startup
            RequestService requestService = kernelImpl.getServiceManager().getServiceByName(
                RequestService.class.getName(), RequestService.class);
            if (requestService == null) {
                throw new IllegalStateException(
                    "Could not get the DSpace RequestService to start the request transaction");
            }

            // Establish a request related to the current session
            // that will trigger the various request listeners
            requestService.startRequest();

            // Run the main() method
            try {
                Object[] arguments = {useargs};

                // Useful for debugging, so left in the code...
                /**System.out.print("About to execute: " + className);
                 for (String param : useargs)
                 {
                 System.out.print(" " + param);
                 }
                 System.out.println("");**/

                Method main = target.getMethod("main", argTypes);
                main.invoke(null, arguments);

                // ensure we close out the request (happy request)
                requestService.endRequest(null);
            } catch (Exception e) {
                // Failure occurred in the request so we destroy it
                requestService.endRequest(e);

                // Exceptions from the script are reported as a 'cause'
                Throwable cause = e.getCause();
                System.err.println("Exception: " + cause.getMessage());
                cause.printStackTrace();
                return 1;
            }
        }

        // Everything completed OK
        return 0;
    }

    /**
     * Load the launcher configuration file
     *
     * @return The XML configuration file Document
     */
    protected static Document getConfig() {
        return getConfig(kernelImpl);
    }

    public static Document getConfig(DSpaceKernelImpl kernelImpl) {
        // Load the launcher configuration file
        String config = kernelImpl.getConfigurationService().getProperty("dspace.dir") +
            System.getProperty("file.separator") + "config" +
            System.getProperty("file.separator") + "launcher.xml";
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = null;
        try {
            doc = saxBuilder.build(config);
        } catch (Exception e) {
            System.err.println("Unable to load the launcher configuration file: [dspace]/config/launcher.xml");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return doc;
    }

    /**
     * Display the commands that are defined in launcher.xml and/or the script service.
     * @param commandConfigs configs as Document
     */
    private static void display(Document commandConfigs) {
        // usage
        System.out.println("Usage: dspace [command-name] {parameters}");

        // commands from launcher.xml
        Collection<Element> launcherCommands = getLauncherCommands(commandConfigs);
        if (launcherCommands.size() > 0) {
            System.out.println("\nCommands from launcher.xml");
            for (Element command : launcherCommands) {
                displayCommand(
                    command.getChild("name").getValue(),
                    command.getChild("description").getValue()
                );
            }
        }

        // commands from script service
        Collection<ScriptConfiguration> serviceCommands = getServiceCommands();
        if (serviceCommands.size() > 0) {
            System.out.println("\nCommands from script service");
            for (ScriptConfiguration command : serviceCommands) {
                displayCommand(
                    command.getName(),
                    command.getDescription()
                );
            }
        }
    }

    /**
     * Display a single command using a fixed format. Used by {@link #display}.
     * @param name the name that can be used to invoke the command
     * @param description the description of the command
     */
    private static void displayCommand(String name, String description) {
        System.out.format(" - %s: %s\n", name, description);
    }

    /**
     * Get a sorted collection of the commands that are specified in launcher.xml. Used by {@link #display}.
     * @param commandConfigs the contexts of launcher.xml
     * @return sorted collection of commands
     */
    private static Collection<Element> getLauncherCommands(Document commandConfigs) {
        // List all command elements
        List<Element> commands = commandConfigs.getRootElement().getChildren("command");

        // Sort the commands by name.
        // We cannot just use commands.sort() because it tries to remove and
        // reinsert Elements within other Elements, and that doesn't work.
        TreeMap<String, Element> sortedCommands = new TreeMap<>();
        for (Element command : commands) {
            sortedCommands.put(command.getChild("name").getValue(), command);
        }

        return sortedCommands.values();
    }

    /**
     * Get a sorted collection of the commands that are defined as beans. Used by {@link #display}.
     * @return sorted collection of commands
     */
    private static Collection<ScriptConfiguration> getServiceCommands() {
        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();

        Context throwAwayContext = new Context();

        throwAwayContext.turnOffAuthorisationSystem();
        List<ScriptConfiguration> scriptConfigurations = scriptService.getScriptConfigurations(throwAwayContext);
        throwAwayContext.restoreAuthSystemState();

        try {
            throwAwayContext.complete();
        } catch (SQLException exception) {
            exception.printStackTrace();
            throwAwayContext.abort();
        }

        scriptConfigurations.sort(Comparator.comparing(ScriptConfiguration::getName));

        return scriptConfigurations;
    }

}
