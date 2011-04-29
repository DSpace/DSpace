/*
 * ScriptLauncher.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, Duraspace.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of Duraspace nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.launcher;

import org.dspace.core.ConfigurationManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.RequestService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import java.util.List;
import java.lang.reflect.Method;

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
     * @throws Exception
     */
    public static void main(String[] args)
    {
        // Check that there is at least one argument
        if (args.length < 1)
        {
            System.err.println("You must provide at least one command argument");
            display();
            System.exit(1);
        }

        // Initalise the service manager kernel
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
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
            String message = "Failure during filter init: " + e.getMessage();
            System.err.println(message + ":" + e);
            throw new RuntimeException(message, e);
        }

        // Parse the configuration file looking for the command entered
        Document doc = getConfig();
        String request = args[0];
        Element root = doc.getRootElement();
        List<Element> commands = root.getChildren("command");
        for (Element command : commands)
        {
            if (request.equalsIgnoreCase(command.getChild("name").getValue()))
            {
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
                            System.exit(1);
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
                        System.exit(1);
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
                        if ("dsrun".equals(request)) x = 2;
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
                    RequestService requestService = kernelImpl.getServiceManager().getServiceByName(RequestService.class.getName(), RequestService.class);
                    if (requestService == null) {
                        throw new IllegalStateException("Could not get the DSpace RequestService to start the request transaction");
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
                        Object output = main.invoke(null, arguments);

                        // ensure we close out the request (happy request)
                        requestService.endRequest(null);


                    }
                    catch (Exception e)
                    {
                        // Failure occurred in the request so we destroy it
                        requestService.endRequest(e);

                    	if (kernelImpl != null)
                        {
                            kernelImpl.destroy();
                            kernelImpl = null;
                        }

                        // Exceptions from the script are reported as a 'cause'
                        Throwable cause = e.getCause();
                        System.err.println("Exception: " + cause.getMessage());
                        cause.printStackTrace();
                        System.exit(1);
                    }

                }

                // Destroy the service kernel
                if (kernelImpl != null)
                {
                    kernelImpl.destroy();
                    kernelImpl = null;
                }

                System.exit(0);
            }
        }

        // Destroy the service kernel if it is still alive
        if (kernelImpl != null)
        {
            kernelImpl.destroy();
            kernelImpl = null;
        }

        // The command wasn't found
        System.err.println("Command not found: " + args[0]);
        display();
        System.exit(1);
    }

    /**
     * Load the launcher configuration file
     *
     * @return The XML configuration file Document
     */
    private static Document getConfig()
    {
        // Load the launcher configuration file
        String config = ConfigurationManager.getProperty("dspace.dir") +
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
            System.exit(1);
        }
        return doc;
    }

    /**
     * Display the commands that the current launcher config file knows about
     */
    private static void display()
    {
        Document doc = getConfig();
        List<Element> commands = doc.getRootElement().getChildren("command");
        System.out.println("Usage: dspace [command-name] {parameters}");
        for (Element command : commands)
        {
            System.out.println(" - " + command.getChild("name").getValue() +
                               ": " + command.getChild("description").getValue());
        }
    }
}
