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
import org.dspace.services.email.oauth2.HttpOAuth2TokenClient;
import org.dspace.services.email.oauth2.OAuth2Exception;
import org.dspace.services.email.oauth2.OAuth2Provider;
import org.dspace.services.email.oauth2.OAuth2ProviderRegistry;
import org.dspace.services.email.oauth2.OAuth2Settings;
import org.dspace.services.email.oauth2.OAuth2TokenManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides mail sending services through JavaMail. If a {@link jakarta.mail.Session}
 * instance is provided through JNDI, it will be used. If not, then a session
 * will be created from DSpace configuration data ({@code mail.server}, etc.)
 *
 * <p>When {@code mail.oauth2.provider} is configured, the session uses the
 * XOAUTH2 mechanism and access tokens are refreshed lazily. When it is not
 * configured, the existing basic-auth or unauthenticated behavior is kept.</p>
 *
 * @author mwood
 * @author aseyedia
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class EmailServiceImpl
    extends Authenticator
    implements EmailService {

    private static final Logger logger = LogManager.getLogger();

    private static final String OAUTH2_PROVIDER_PROPERTY = "mail.oauth2.provider";

    private static final String[] OAUTH2_SETTING_KEYS = {
        "mail.oauth2.clientId",
        "mail.oauth2.clientSecret",
        "mail.oauth2.refreshToken",
        "mail.oauth2.tenantId",
        "mail.oauth2.scope",
        "mail.oauth2.tokenEndpoint",
        "mail.oauth2.gmail.clientId",
        "mail.oauth2.gmail.clientSecret",
        "mail.oauth2.gmail.refreshToken",
        "mail.oauth2.microsoft.clientId",
        "mail.oauth2.microsoft.clientSecret",
        "mail.oauth2.microsoft.refreshToken",
        "mail.oauth2.microsoft.tenantId"
    };

    private Session session;

    private ConfigurationService cfg;

    private OAuth2TokenManager oauth2TokenManager;

    private OAuth2Settings oauth2Settings;

    private OAuth2Exception oauth2ConfigurationError;

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
    // JNDI usage is safe here as it looks up a configured mail session resource, not user input.
    @SuppressWarnings("BanJNDI")
    public void init() {
        oauth2TokenManager = null;
        oauth2Settings = null;
        oauth2ConfigurationError = null;

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
            if (OAuth2ProviderRegistry.isConfigured(cfg)) {
                logger.warn(
                    "OAuth2 SMTP settings are configured but a JNDI mail session was found. "
                        + "The JNDI session takes precedence and OAuth2 settings are ignored."
                );
            }
            return;
        }

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
        if (extras != null) {
            for (String argument : extras) {
                String key = argument.substring(0, argument.indexOf('=')).trim();
                String value = argument.substring(argument.indexOf('=') + 1).trim();
                props.put(key, value);
            }
        }

        String providerId = StringUtils.trimToNull(cfg.getProperty(OAUTH2_PROVIDER_PROPERTY));
        if (providerId == null) {
            if (hasOAuth2Settings()) {
                logger.warn(
                    "OAuth2 settings were found but {} is not set. "
                        + "OAuth2 is ignored and the existing mail configuration is used.",
                    OAUTH2_PROVIDER_PROPERTY
                );
            }
            initLegacySession(props);
            return;
        }

        initOAuth2Session(props, providerId);
    }

    private void initLegacySession(Properties props) {
        String username = cfg.getProperty("mail.server.username");
        if (StringUtils.isNotBlank(username)) {
            props.put("mail.smtp.auth", "true");
            session = Session.getInstance(props, this);
            logger.info("Initialized SMTP session with basic auth (username/password).");
        } else {
            session = Session.getInstance(props);
            logger.info("Initialized SMTP session without authentication.");
        }
    }

    private void initOAuth2Session(Properties props, String providerId) {
        try {
            OAuth2Provider provider = OAuth2ProviderRegistry.getProvider(providerId);
            oauth2Settings = provider.loadSettings(cfg);
            oauth2TokenManager = new OAuth2TokenManager(new HttpOAuth2TokenClient(), oauth2Settings);
            enableXoauth2(props);
            session = Session.getInstance(props, this);
            logger.info(
                "Initialized SMTP session with OAuth2 provider '{}'; access tokens will be fetched lazily.",
                providerId
            );
        } catch (OAuth2Exception e) {
            oauth2ConfigurationError = e;
            enableXoauth2(props);
            session = Session.getInstance(props, this);
            logger.error(
                "Invalid OAuth2 configuration for provider '{}': {}",
                providerId,
                e.getMessage()
            );
        }
    }

    private void enableXoauth2(Properties props) {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        props.put("mail.smtp.auth.login.disable", "true");
        props.put("mail.smtp.auth.plain.disable", "true");
    }

    private boolean hasOAuth2Settings() {
        for (String key : OAUTH2_SETTING_KEYS) {
            if (StringUtils.isNotBlank(cfg.getProperty(key))) {
                return true;
            }
        }
        return false;
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
     * Falls back to plain username/password only when OAuth2 is not configured.
     *
     * @return a {@link PasswordAuthentication} instance for JavaMail
     * @throws OAuth2Exception when OAuth2 is configured but cannot issue a token
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (cfg == null) {
            cfg = DSpaceServicesFactory.getInstance().getConfigurationService();
        }

        if (oauth2ConfigurationError != null) {
            throw oauth2ConfigurationError;
        }

        if (oauth2TokenManager != null && oauth2Settings != null) {
            return new PasswordAuthentication(
                oauth2Settings.username(),
                oauth2TokenManager.getAccessToken()
            );
        }

        return new PasswordAuthentication(
            cfg.getProperty("mail.server.username"),
            cfg.getProperty("mail.server.password")
        );
    }

    /**
     * Force a new initialization of the session, useful for testing purpose
     */
    public void reset() {
        session = null;
        oauth2TokenManager = null;
        oauth2Settings = null;
        oauth2ConfigurationError = null;
        init();
    }
}
