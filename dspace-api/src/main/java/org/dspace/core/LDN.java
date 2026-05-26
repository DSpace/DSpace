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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.secure.SecureFileAccess;

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

    /** Velocity template for the message*/
    private Template template;

    /** Allowed base directory for LDN messages / templates **/
    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final String dspaceDir = configurationService.getProperty("dspace.dir", "/dspace");
    private static final String[] DEFAULT_TEMPLATE_PATHS = new String[]{
        dspaceDir + File.separatorChar + "config" + File.separatorChar + "ldn"};

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
        VelocityEngine templateEngine = new VelocityEngine();
        templateEngine.init(Utils.getSecureVelocityProperties(RESOURCE_REPOSITORY_NAME));

        VelocityContext vctx = new VelocityContext();
        // Pass a restricted (via configuration) list of resolved Configuration keys and values, for
        // template lookup
        vctx.put("config", Utils.getAllowedTemplateConfig());
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
        List<String> allowedBasePaths = List.of(
                Arrays.stream(configurationService
                                .getArrayProperty("ldn.template.path", DEFAULT_TEMPLATE_PATHS))
                        .findFirst()
                        .orElseThrow(() -> new IOException("No LDN template path configured"))
        );
        String ldnFilePath = SecureFileAccess.calculateAbsolutePathUsingBaseDir(ldnMessageFile,
                allowedBasePaths.get(0));
        try (
            InputStream is = SecureFileAccess.getInputStream(ldnFilePath, allowedBasePaths, "ldn");
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
}
