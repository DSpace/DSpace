package org.dspace.solr;

import java.io.File;
import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

/**
 * @author mdiggory
 * 
 */
public class SolrConfigurator implements ServletContextAware, InitializingBean {

	private static Log log = LogFactory.getLog(SolrConfigurator.class);

	private ServletContext servletContext;

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void afterPropertiesSet() throws Exception {

		try {
			/*
			 * look for solr home in the locations it expects to see them on
			 * startup.
			 */

			String solrHome = System.getProperty("solr.solr.home");
			if (solrHome == null) {
				try {
					InitialContext ctx = new InitialContext();
					solrHome = (String) ctx.lookup("java:comp/env/solr/home");
				} catch (NamingException e) {
					if (log.isDebugEnabled())
						log.debug(e.getMessage(), e);
					else
						log.info(e.getMessage());
				}
			}

			if (solrHome == null) {
				solrHome = new File("./solr").getAbsolutePath();
			}

			File home = new File(solrHome);
			File conf = new File(home, "solr.xml");

			if (!conf.exists() && this.servletContext != null) {
				log.info("Installing solr into:" + solrHome);
				home.mkdirs();

				URL build = this.servletContext.getResource("/WEB-INF/conf/build.xml");
				
				Project p = new Project();
				//p.setBasedir(build.toExternalForm());//this.servletContext.getRealPath(""));//.getResource("").toExternalForm());//solrHome);
				p.setUserProperty("ant.file", build.toExternalForm());
				p.setUserProperty("solr.home", solrHome);
				p.setUserProperty("servlet.context", this.servletContext.getResource("/").toExternalForm());
				DefaultLogger consoleLogger = new DefaultLogger();
				consoleLogger.setErrorPrintStream(System.err);
				consoleLogger.setOutputPrintStream(System.out);
				consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
				p.addBuildListener(consoleLogger);

				p.fireBuildStarted();
				p.init();
				ProjectHelper helper = ProjectHelper.getProjectHelper();
				p.addReference("ant.projectHelper", helper);
				helper.parse(p, build);
				p.executeTarget(p.getDefaultTarget());
				p.fireBuildFinished(null);
			} else {
				log.info("Solr Configuration already installed: " + conf);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}