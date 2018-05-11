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
import java.util.List;
import java.util.TreeMap;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.RequestService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * A DSpace script launcher.
 *
 * @author Stuart Lewis
 * @author Mark Diggory
 */
public class ScriptLauncher
{
    /** The service manager kernel */
    private static transient DSpaceKernelImpl kernelImpl;

    /**
     * Execute the DSpace script launcher
     *
     * @param args Any parameters required to be passed to the scripts it executes
     * @throws IOException if IO error
     * @throws FileNotFoundException if file doesn't exist
     */
    public static void main(String[] args)
            throws FileNotFoundException, IOException
    {
        // Initialise the service manager kernel
        try
        {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                kernelImpl.start();
            }
        } catch (Exception e)
        {
            // Failed to start so destroy it and log and throw an exception
            try
            {
                kernelImpl.destroy();
            }
            catch (Exception e1)
            {
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
        if (args.length < 1)
        {
            System.err.println("You must provide at least one command argument");
            display(commandConfigs);
            System.exit(1);
        }

        // Look up command in the configuration, and execute.
        int status;
        status = runOneCommand(commandConfigs, args);

        // Destroy the service kernel if it is still alive
        if (kernelImpl != null)
        {
            kernelImpl.destroy();
            kernelImpl = null;
        }

        System.exit(status);
    }

    /**
     * Recognize and execute a single command.
     * @param doc Document
     * @param args arguments
     */
    static int runOneCommand(Document commandConfigs, String[] args)
    {
        String request = args[0];
        Element root = commandConfigs.getRootElement();
        List<Element> commands = root.getChildren("command");
        Element command = null;
        for (Element candidate : commands)
        {
            if (request.equalsIgnoreCase(candidate.getChild("name").getValue()))
            {
                command = candidate;
                break;
            }
        }

        if (null == command)
        {
            // The command wasn't found
            System.err.println("Command not found: " + args[0]);
            display(commandConfigs);
            return 1;
        }

        // Run each step
        List<Element> steps = command.getChildren("step");
        for (Element step : steps)
        {
            // Instantiate the class
            Class target = null;

            // Is it the special case 'dsrun' where the user provides the class name?
            String className;
            if ("dsrun".equals(request))
            {
                if (args.length < 2)
                {
                    System.err.println("Error in launcher.xml: Missing class name");
                    return 1;
                }
                className = args[1];
            }
            else {
                className = step.getChild("class").getValue();
            }
            try
            {
                target = Class.forName(className,
                                       true,
                                       Thread.currentThread().getContextClassLoader());
            }
            catch (ClassNotFoundException e)
            {
                System.err.println("Error in launcher.xml: Invalid class name: " + className);
                return 1;
            }

            // Strip the leading argument from the args, and add the arguments
            // Set <passargs>false</passargs> if the arguments should not be passed on
            String[] useargs = args.clone();
            Class[] argTypes = {useargs.getClass()};
            boolean passargs = true;
            if ((step.getAttribute("passuserargs") != null) &&
                ("false".equalsIgnoreCase(step.getAttribute("passuserargs").getValue())))
            {
                passargs = false;
            }
            if ((args.length == 1) || (("dsrun".equals(request)) && (args.length == 2)) || (!passargs))
            {
                useargs = new String[0];
            }
            else
            {
                // The number of arguments to ignore
                // If dsrun is the command, ignore the next, as it is the class name not an arg
                int x = 1;
                if ("dsrun".equals(request))
                {
                    x = 2;
                }
                String[] argsnew = new String[useargs.length - x];
                for (int i = x; i < useargs.length; i++)
                {
                    argsnew[i - x] = useargs[i];
                }
                useargs = argsnew;
            }

            // Add any extra properties
            List<Element> bits = step.getChildren("argument");
            if (step.getChild("argument") != null)
            {
                String[] argsnew = new String[useargs.length + bits.size()];
                int i = 0;
                for (Element arg : bits)
                {
                    argsnew[i++] = arg.getValue();
                }
                for (; i < bits.size() + useargs.length; i++)
                {
                    argsnew[i] = useargs[i - bits.size()];
                }
                useargs = argsnew;
            }

            // Establish the request service startup
            RequestService requestService = kernelImpl.getServiceManager().getServiceByName(
                    RequestService.class.getName(), RequestService.class);
            if (requestService == null)
            {
                throw new IllegalStateException(
                        "Could not get the DSpace RequestService to start the request transaction");
            }

            // Establish a request related to the current session
            // that will trigger the various request listeners
            requestService.startRequest();

            // Run the main() method
            try
            {
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
            }
            catch (Exception e)
            {
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
    protected static Document getConfig()
    {
        // Load the launcher configuration file
        String config = kernelImpl.getConfigurationService().getProperty("dspace.dir") +
                        System.getProperty("file.separator") + "config" +
                        System.getProperty("file.separator") + "launcher.xml";
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = null;
        try
        {
            doc = saxBuilder.build(config);
        }
        catch (Exception e)
        {
            System.err.println("Unable to load the launcher configuration file: [dspace]/config/launcher.xml");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return doc;
    }

    /**
     * Display the commands that the current launcher config file knows about
     * @param commandConfigs configs as Document
     */
    private static void display(Document commandConfigs)
    {
        // List all command elements
        List<Element> commands = commandConfigs.getRootElement().getChildren("command");

        // Sort the commands by name.
        // We cannot just use commands.sort() because it tries to remove and
        // reinsert Elements within other Elements, and that doesn't work.
        TreeMap<String, Element> sortedCommands = new TreeMap<>();
        for (Element command : commands)
        {
            sortedCommands.put(command.getChild("name").getValue(), command);
        }

        // Display the sorted list
        System.out.println("Usage: dspace [command-name] {parameters}");
        for (Element command : sortedCommands.values())
        {
            System.out.println(" - " + command.getChild("name").getValue() +
                               ": " + command.getChild("description").getValue());
        }
    }
}
