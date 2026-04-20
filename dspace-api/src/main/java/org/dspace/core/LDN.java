/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class representing an LDN message json
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDN {
    /**
     * The content of the ldn message
     */
    private String content;
    private String contentName;

    /**
     * The arguments to fill out
     */
    private final List<Object> arguments;

    private static final Logger LOG = LogManager.getLogger();

    /** Velocity template settings. */
    private static final String RESOURCE_REPOSITORY_NAME = "LDN";
    private static final Properties VELOCITY_PROPERTIES = new Properties();
    static {
        VELOCITY_PROPERTIES.put(Velocity.RESOURCE_LOADERS, "string");
        VELOCITY_PROPERTIES.put("resource.loader.string.description",
                "Velocity StringResource loader");
        VELOCITY_PROPERTIES.put("resource.loader.string.class",
                StringResourceLoader.class.getName());
        VELOCITY_PROPERTIES.put("resource.loader.string.repository.name",
                RESOURCE_REPOSITORY_NAME);
        VELOCITY_PROPERTIES.put("resource.loader.string.repository.static",
                "false");
    }

    /** Velocity template for the message*/
    private Template template;

    /**
     * Create a new ldn message.
     */
    public LDN() {
        arguments = new ArrayList<>(20);
        template = null;
        content = EMPTY;
    }

    /**
     * Set the content of the message. Setting this also "resets" the message
     * formatting - <code>addArgument</code> will start over. Comments and any
     * "Subject:" line must be stripped.
     *
     * @param name a name for this message
     * @param cnt the content of the message
     */
    public void setContent(String name, String cnt) {
        content = cnt;
        contentName = name;
        arguments.clear();
    }

    /**
     * Fill out the next argument in the template
     *
     * @param arg the value for the next argument
     */
    public void addArgument(Object arg) {
        arguments.add(arg);
    }

    /**
     * Generates the ldn message.
     *
     * @throws MessagingException if there was a problem sending the mail.
     * @throws IOException        if IO error
     */
    public String generateLDNMessage() {
        ConfigurationService config
            = DSpaceServicesFactory.getInstance().getConfigurationService();

        VelocityEngine templateEngine = new VelocityEngine();
        templateEngine.init(VELOCITY_PROPERTIES);

        VelocityContext vctx = new VelocityContext();
        vctx.put("config", new LDN.UnmodifiableConfigurationService(config));
        vctx.put("params", Collections.unmodifiableList(arguments));

        if (null == template) {
            if (StringUtils.isBlank(content)) {
                LOG.error("template has no content");
                throw new RuntimeException("template has no content");
            }
            // No template, so use a String of content.
            StringResourceRepository repo = (StringResourceRepository)
                templateEngine.getApplicationAttribute(RESOURCE_REPOSITORY_NAME);
            repo.putStringResource(contentName, content);
            // Turn content into a template.
            template = templateEngine.getTemplate(contentName);
        }

        StringWriter writer = new StringWriter();
        try {
            template.merge(vctx, writer);
        } catch (MethodInvocationException | ParseErrorException
                | ResourceNotFoundException ex) {
            LOG.error("Template not merged:  {}", ex.getMessage());
            throw new RuntimeException("Template not merged", ex);
        }
        return writer.toString();
    }

    /**
     * Get the VTL template for a ldn message. The message is suitable
     * for inserting values using Apache Velocity.
     *
     * @param ldnMessageFile
     *            full name for the ldn template, for example "/dspace/config/ldn/request-review".
     *
     * @return the ldn object, configured with body.
     *
     * @throws IOException if IO error,
     *                     if the template couldn't be found, or there was some other
     *                     error reading the template
     */
    public static LDN getLDNMessage(String ldnMessageFile)
        throws IOException {
        StringBuilder contentBuffer = new StringBuilder();
        try (
            InputStream is = new FileInputStream(ldnMessageFile);
            InputStreamReader ir = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(ir);
            ) {
            boolean more = true;
            while (more) {
                String line = reader.readLine();
                if (line == null) {
                    more = false;
                } else {
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        }
        LDN ldn = new LDN();
        ldn.setContent(ldnMessageFile, contentBuffer.toString());
        return ldn;
    }

    /**
     * Wrap ConfigurationService to prevent templates from modifying
     * the configuration.
     */
    public static class UnmodifiableConfigurationService {
        private final ConfigurationService configurationService;

        /**
         * Swallow an instance of ConfigurationService.
         *
         * @param cs the real instance, to be wrapped.
         */
        public UnmodifiableConfigurationService(ConfigurationService cs) {
            configurationService = cs;
        }

        /**
         * Look up a key in the actual ConfigurationService.
         *
         * @param key to be looked up in the DSpace configuration.
         * @return whatever value ConfigurationService associates with {@code key}.
         */
        public String get(String key) {
            return configurationService.getProperty(key);
        }
    }
}
