/*
 */
package org.datadryad.rest.servlet;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadAPIServletContainer extends ServletContainer {
    private final static Logger log = Logger.getLogger(DryadAPIServletContainer.class);
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("initializing DryadAPIServletContainer - loading dspace config");
        String configFile = config.getServletContext().getInitParameter("dspace.config");
        ConfigurationManager.loadConfig(configFile);
        log.info("initialized DryadAPIServletContainer");
        // Must load configFile before initializing, some singletons depend on
        // configured DSpace
        super.init(config);
    }
}
