/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email;

import java.io.IOException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
 * Provides mail sending services through JavaMail. If a
 * {@link jakarta.mail.Session}
 * instance is provided through JNDI, it will be used. If not, then a session
 * will be created from DSpace configuration data ({@code mail.server}, etc.)
 *
 * Added OAuth2 Gmail support via refresh tokens.
 *
 * See <a href="https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html">JavaMail SMTP API</a>
 * for more details on all available SMTP parameters.
 *
 * @author mwoodiupui
 * @author aseyedia
 */
public class EmailServiceImpl
        extends Authenticator
        implements EmailService {
    private static final Logger logger = LogManager.getLogger();

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory GSON_FACTORY = GsonFactory.getDefaultInstance();

    private Session session;
    private ConfigurationService cfg;

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
    @SuppressWarnings("BanJNDI")
    public void init() {
        // See if there is already a Session in our environment
        String sessionName = cfg.getProperty("mail.session.name", "Session");
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
            if (StringUtils.isNotBlank(host)) {
                props.put("mail.smtp.host", host);
            }
            String port = cfg.getProperty("mail.server.port");
            if (StringUtils.isNotBlank(port)) {
                props.put("mail.smtp.port", port);
            }
            // Set extra configuration properties
            String[] extras = cfg.getArrayProperty("mail.extraproperties");
            if (extras != null && extras.length > 0) {
                if (extras != null) {
                    String key;
                    String value;
                    for (String argument : extras) {
                        key = argument.substring(0, argument.indexOf('=')).trim();
                        value = argument.substring(argument.indexOf('=') + 1).trim();
                        props.put(key, value);
                    }
                }
                // 3) Try OAuth2 with Refresh Token flow
                logger.debug("Checking for OAuth2 credentials");

                // Try to get credentials from environment variables first, then fall back to
                // config properties
                String clientId = StringUtils.defaultIfBlank(System.getenv("OAUTH2_CLIENT_ID"),
                        cfg.getProperty("mail.oauth2.clientId"));
                String clientSecret = StringUtils.defaultIfBlank(System.getenv("OAUTH2_CLIENT_SECRET"),
                        cfg.getProperty("mail.oauth2.clientSecret"));
                String refreshToken = StringUtils.defaultIfBlank(System.getenv("OAUTH2_REFRESH_TOKEN"),
                        cfg.getProperty("mail.oauth2.refreshToken"));

                if (logger.isDebugEnabled()) {
                    logger.debug("OAuth2 client ID available: {}", StringUtils.isNotBlank(clientId));
                    logger.debug("OAuth2 client secret available: {}", StringUtils.isNotBlank(clientSecret));
                    logger.debug("OAuth2 refresh token available: {}", StringUtils.isNotBlank(refreshToken));
                }

                if (StringUtils.isNotBlank(clientId)
                        && StringUtils.isNotBlank(clientSecret)
                        && StringUtils.isNotBlank(refreshToken)) {
                    try {
                        // fetch a fresh access token
                        String accessToken = new GoogleRefreshTokenRequest(
                                HTTP_TRANSPORT, GSON_FACTORY,
                                refreshToken, clientId, clientSecret).execute().getAccessToken();

                        // cache it for JavaMail
                        cfg.setProperty("mail.server.oauth2.token", accessToken);

                        // set up XOAUTH2
                        props.put("mail.smtp.auth", "true");
                        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
                        props.put("mail.smtp.auth.login.disable", "true");
                        props.put("mail.smtp.auth.plain.disable", "true");

                        session = Session.getInstance(props, this);
                        logger.info("Initialized SMTP session with OAuth2 token");
                        return;

                    } catch (IOException e) {
                        logger.error("Failed to refresh Gmail OAuth2 token", e);
                    }
                }

                // 4) Fallback to basic username/password
                String username = cfg.getProperty("mail.server.username");
                if (StringUtils.isNotBlank(username)) {
                    props.put("mail.smtp.auth", "true");
                    session = Session.getInstance(props, this);
                    logger.info("Initialized SMTP session with basic auth (username/password)");
                } else {
                    session = Session.getInstance(props);
                    logger.info("Initialized SMTP session without authentication");
                }
            }
        }
    }

    /**
     * Provides credentials for SMTP authentication.
     * <p>
     * When using XOAUTH2, the access token is passed as the password. This is
     * the expected behavior per the JavaMail documentation:
     * <a href=
     * "https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html">
     * JavaMail SMTP Provider Overview</a>:
     * <blockquote>
     * The OAuth 2.0 Access Token should be passed as the password for this
     * mechanism.
     * </blockquote>
     * Falls back to plain username/password if no OAuth2 token is configured.
     *
     * @return a {@link PasswordAuthentication} instance for JavaMail
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (cfg == null) {
            cfg = DSpaceServicesFactory.getInstance().getConfigurationService();
        }

        // XOAUTH2 uses accessToken as "password"
        String oauth2 = cfg.getProperty("mail.server.oauth2.token");
        if (StringUtils.isNotBlank(oauth2)) {
            return new PasswordAuthentication(
                    cfg.getProperty("mail.server.username"),
                    oauth2);
        }

        // fallback to plain password
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
