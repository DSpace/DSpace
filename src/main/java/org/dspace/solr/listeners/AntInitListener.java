package org.dspace.solr.listeners;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * @author mdiggory
 *
 */
public class AntInitListener implements ServletContextListener
{

	private static Log log = LogFactory.getLog(AntInitListener.class);
	
    public void contextDestroyed(ServletContextEvent sce)
    {
        // TODO Auto-generated method stub

    }

    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
        	
        	String solrHome = System.getProperty("solr.solr.home");
        	
        	if(solrHome == null)
        	{
				try {
					InitialContext ctx = new InitialContext();
					solrHome = (String) ctx.lookup("java:comp/env/solr/home");
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
					log.debug(e.getMessage(),e);
				}
        	}
 
            if(solrHome == null)
            {
            	solrHome = new File("./solr").getAbsolutePath();//System.getProperty("user.dir");
            }
            

        	log.info("installing solr into:" + solrHome);
        	
            File home = new File(solrHome);
            
            home.mkdirs();
            
            File conf = new File(home, "solr.xml");
            
            if (!conf.exists())
            {
                Project p = new Project();
                p.setBasedir(sce.getServletContext().getRealPath(""));
                p.setUserProperty("ant.file", sce.getServletContext().getRealPath("/WEB-INF/build.xml"));
                p.setProperty("solr.home", solrHome);
                
                p.init();
                ProjectHelper helper = ProjectHelper.getProjectHelper();
                p.addReference("ant.projectHelper", helper);
                helper.parse(p, sce.getServletContext().getResource("/WEB-INF/build.xml"));
                p.executeTarget(p.getDefaultTarget());
                
            }
               
            
        }
        catch (MalformedURLException e)
        {
            log.error(e.getMessage(),e);
        }
        catch (IOException e)
        {
        	log.error(e.getMessage(),e);
        }

    }
}
