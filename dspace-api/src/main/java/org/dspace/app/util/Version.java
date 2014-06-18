/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Display information about this DSpace, its environment, and how it was built.
 *
 * @author mwood
 */
public class Version
{
    public static void main(String[] argv)
        throws IOException
    {
        InputStream propStream;

        Properties sys = System.getProperties();

        // DSpace version
        System.out.printf("DSpace version:  %s\n",
                          Util.getSourceVersion());

        // SCM revision
        Properties scm = new Properties();
        propStream = Version.class.getResourceAsStream("/scm.properties");
        if (null != propStream)
        {
            scm.load(propStream);
        }
        System.out.printf("  SCM revision:  %s\n", scm.get("revision"));
        System.out.printf("    SCM branch:  %s\n", scm.get("branch"));

        // OS version
        System.out.printf("            OS:  %s(%s) version %s\n",
                          sys.get("os.name"),
                          sys.get("os.arch"),
                          sys.get("os.version"));

        // UIs used
        List<AbstractDSpaceWebapp> apps = AbstractDSpaceWebapp.getApps();
        System.out.println("  Applications:");
        for (AbstractDSpaceWebapp app : apps)
        {
            System.out.printf("                %s at %s\n",
                    app.getKind(), app.getURL());
        }

        // Is Discovery available?
        ConfigurationService config = new DSpace().getConfigurationService();
        String consumers = config.getPropertyAsType("event.dispatcher.default.consumers", ""); // Avoid null pointer
        List<String> consumerList = Arrays.asList(consumers.split("\\s*,\\s*"));
        if (consumerList.contains("discovery"))
        {
            System.out.println("     Discovery:  enabled.");
        }

        // Is Lucene search enabled?
        if (consumerList.contains("search"))
        {
            System.out.println(" Lucene search:  enabled.");
        }

        // Java version
        System.out.printf("           JRE:  %s version %s\n",
                          sys.get("java.vendor"),
                          sys.get("java.version"));

        // ant version
        Properties ant = new Properties();
        propStream = Version.class.getResourceAsStream("/ant.properties");
        if (null != propStream)
        {
            ant.load(propStream);
        }
        System.out.printf("   Ant version:  %s\n",
                          ant.get("ant.version"));

        // maven version
        Properties maven = new Properties();
        propStream = Version.class.getResourceAsStream("/maven.properties");
        if (null != propStream)
        {
            maven.load(propStream);
        }
        System.out.printf(" Maven version:  %s\n",
                          maven.get("maven.version"));

        // DSpace directory path
        System.out.printf("   DSpace home:  %s\n",
                          config.getProperty("dspace.dir"));
    }
}
