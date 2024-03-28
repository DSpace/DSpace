/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.lang.reflect.InvocationTargetException;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Class that registers the web application upon startup of the application.
 *
 * @author kevinvandevelde at atmire.com
 */
public class DSpaceWebappListener implements ServletContextListener {

    private AbstractDSpaceWebapp webApp;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        /*
         * Register that this application is running.
         */

        try {
            Class webappClass = Class.forName("org.dspace.utils.DSpaceWebapp");
            webApp = (AbstractDSpaceWebapp) webappClass.getDeclaredConstructor().newInstance();
            webApp.register();
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | InvocationTargetException ex) {
            event.getServletContext().log("Can't create webapp MBean:  " + ex.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        webApp.deregister();
    }
}
