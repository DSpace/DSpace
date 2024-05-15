/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email;

import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import jakarta.annotation.PostConstruct;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides mail sending services through JavaMail.  If a {@link jakarta.mail.Session}
 * instance is provided through JNDI, it will be used.  If not, then a session
 * will be created from DSpace configuration data ({@code mail.server} etc.)
 *
 * @author mwood
 */
public class EmailServiceImpl
    extends Authenticator
    implements EmailService {
    private static final Logger logger = LogManager.getLogger();

    private Session session = null;

    private ConfigurationService cfg = null;

    /**
     * Inject/set the ConfigurationService
     *
     * @param cfg the configurationService object
     */
    @Autowired(required = true)
    public void setCfg(ConfigurationService cfg) {
        this.cfg = cfg;
    }

    /**
     * Provide a reference to the JavaMail session.
     *
     * @return the managed Session, or {@code null} if none could be created.
     */
    @Override
    public Session getSession() {
        return session;
    }

    @PostConstruct
    public void init() {
        // See if there is already a Session in our environment
        String sessionName = cfg.getProperty("mail.session.name");
        if (null == sessionName) {
            sessionName = "Session";
        }
        String sessionUri = "java:comp/env/mail/" + sessionName;
        logger.debug("Looking up Session as {}", sessionUri);
        try {
            InitialContext ctx = new InitialContext(null);
            session = (Session) ctx.lookup(sessionUri);
        } catch (NameNotFoundException | NoInitialContextException ex) {
            // Not a problem -- build a new Session from configuration.
        } catch (NamingException ex) {
            logger.warn("Couldn't get an email session from environment:  {}:  {}",
                        ex.getClass().getName(), ex.getMessage());
        }

        if (null != session) {
            logger.info("Email session retrieved from environment.");
        } else { // No Session provided, so create one
            logger.info("Initializing an email session from configuration.");
            Properties props = new Properties();
            props.put("mail.transport.protocol", "smtp");
            String host = cfg.getProperty("mail.server");
            if (null != host) {
                props.put("mail.host", cfg.getProperty("mail.server"));
            }
            String port = cfg.getProperty("mail.server.port");
            if (null != port) {
                props.put("mail.smtp.port", port);
            }
            // Set extra configuration properties
            String[] extras = cfg.getArrayProperty("mail.extraproperties");
            if (extras != null) {
                String key;
                String value;
                for (String argument : extras) {
                    key = argument.substring(0, argument.indexOf('=')).trim();
                    value = argument.substring(argument.indexOf('=') + 1).trim();
                    props.put(key, value);
                }
            }
            if (StringUtils.isBlank(cfg.getProperty("mail.server.username"))) {
                session = Session.getInstance(props);
            } else {
                props.put("mail.smtp.auth", "true");
                session = Session.getInstance(props, this);
            }
        }
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (null == cfg) {
            cfg = DSpaceServicesFactory.getInstance().getConfigurationService();
        }

        return new PasswordAuthentication(
            cfg.getProperty("mail.server.username"),
            cfg.getProperty("mail.server.password"));
    }

    /**
     * Force a new initialization of the session, useful for testing purpose
     */
    public void reset() {
        session = null;
        init();
    }
}
