/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.service.EmailTemplateService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link EmailTemplateService}.
 *
 * <p>Handles loading, parsing, validating, and saving email templates stored as VTL
 * files in {@code {dspace.dir}/config/emails/}.</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private static final Logger log = LogManager.getLogger(EmailTemplateServiceImpl.class);

    private static final String VELOCITY_REPO_NAME = "emailTemplateValidationRepo";
    private static final int MIN_CONTENT_LENGTH = 10;
    private static final int MAX_CONTENT_LENGTH = 50000;

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern SUBJECT_PATTERN = Pattern.compile(
        "^\\s*#set\\s*\\(\\s*\\$subject\\s*=\\s*[\"'](.*?)[\"']\\s*\\)", Pattern.MULTILINE);
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(
        "^\\s*#set\\s*\\(\\s*\\$mimetype\\s*=\\s*[\"'](text/[a-zA-Z0-9._-]+)[\"']\\s*\\)", Pattern.MULTILINE);
    private static final Pattern PARAM_PATTERN = Pattern.compile(
        "^##(?:\\s*Parameters:)?\\s*\\{?(\\d+)\\}?\\s+(.*?)$");

    private static final PolicyFactory HTML_POLICY = Sanitizers.FORMATTING
        .and(Sanitizers.BLOCKS)
        .and(Sanitizers.LINKS)
        .and(Sanitizers.STYLES)
        .and(Sanitizers.TABLES);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * Default constructor.
     */
    public EmailTemplateServiceImpl() {
    }

    /**
     * Gets the directory where email templates are stored.
     *
     * @return the File representing the emails directory
     */
    protected File getEmailsDirectory() {
        String dspaceDir = configurationService.getProperty("dspace.dir");
        if (dspaceDir == null) {
            throw new IllegalStateException("Configuration property 'dspace.dir' is not set.");
        }
        return new File(dspaceDir, "config" + File.separator + "emails");
    }

    @Override
    public List<EmailTemplate> findAll(Context context) throws IOException, SQLException, AuthorizeException {
        if (context == null || !authorizeService.isAdmin(context)) {
            logUnauthorizedAttempt(context, "retrieve all email templates");
            throw new AuthorizeException("Only administrators are allowed to access email templates.");
        }

        File dir = getEmailsDirectory();
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("Email templates directory does not exist or is not a directory: {}", dir.getAbsolutePath());
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) -> !name.startsWith(".") && new File(d, name).isFile());
        if (files == null) {
            return Collections.emptyList();
        }

        List<EmailTemplate> templates = new ArrayList<>();
        for (File file : files) {
            try {
                EmailTemplate template = parseTemplateFile(file);
                if (template != null) {
                    templates.add(template);
                }
            } catch (IOException e) {
                log.error("Failed to read email template file: {}", file.getName(), e);
            }
        }

        templates.sort(Comparator.comparing(EmailTemplate::getName));
        return templates;
    }

    /**
     * Resolves the template file, ensuring it resides within the canonical emails directory
     * and is protected against path traversal and symlink escapes.
     *
     * @param name the template filename
     * @return the resolved File
     * @throws IOException if directory canonical resolution fails
     * @throws IllegalArgumentException if the name is invalid or attempts path traversal
     */
    protected File resolveTemplateFile(String name) throws IOException {
        validateTemplateName(name);

        File dir = getEmailsDirectory();
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("Email templates directory does not exist or is not a directory: {}", dir.getAbsolutePath());
            throw new IllegalStateException("Email templates directory does not exist or is not a directory: "
                    + dir.getAbsolutePath());
        }

        Path canonicalBase = dir.toPath().toRealPath();
        Path targetPath = canonicalBase.resolve(name).normalize();

        if (!targetPath.startsWith(canonicalBase)) {
            log.warn("Path traversal attempt detected: {}", name);
            throw new IllegalArgumentException("Path traversal attempt detected: " + name);
        }

        if (Files.exists(targetPath)) {
            Path realTargetPath = targetPath.toRealPath();
            if (!realTargetPath.startsWith(canonicalBase)) {
                log.warn("Symlink escape attempt detected: {}", name);
                throw new IllegalArgumentException("Symlink escape attempt detected: " + name);
            }
        }

        return targetPath.toFile();
    }

    @Override
    public EmailTemplate findByName(Context context, String name)
            throws IOException, SQLException, AuthorizeException {
        if (context == null || !authorizeService.isAdmin(context)) {
            logUnauthorizedAttempt(context, "retrieve email template '" + name + "'");
            throw new AuthorizeException("Only administrators are allowed to access email templates.");
        }

        File file = resolveTemplateFile(name);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        return parseTemplateFile(file);
    }

    @Override
    public EmailTemplate update(Context context, String name, String content)
            throws IOException, SQLException, AuthorizeException {
        if (context == null || !authorizeService.isAdmin(context)) {
            logUnauthorizedAttempt(context, "update email template '" + name + "'");
            throw new AuthorizeException("Only administrators are allowed to update email templates.");
        }

        File file = resolveTemplateFile(name);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Template does not exist: " + name);
        }

        validateContent(name, content);

        String trimmedContent = content.strip() + "\n";
        Files.writeString(file.toPath(), trimmedContent, StandardCharsets.UTF_8);

        log.info(LogHelper.getHeader(context, "email_template_update",
                "Email template has been updated: " + name));

        return parseTemplateFile(file);
    }

    @Override
    public String computeETag(String content) {
        if (content == null) {
            return "";
        }
        return DigestUtils.sha256Hex(content);
    }

    @Override
    public void validateContent(String name, String content) {
        if (content == null || content.strip().length() < MIN_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                "Template content must contain at least " + MIN_CONTENT_LENGTH + " non-whitespace characters");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                "Template content exceeds maximum allowed length of " + MAX_CONTENT_LENGTH + " characters");
        }

        validateHtmlSafety(content);

        // Validate VTL syntax with VelocityEngine
        validateVelocitySyntax(name, content);
    }

    /**
     * Validates that any HTML content conforms to the safe HTML policy using the
     * OWASP Java HTML Sanitizer.
     *
     * @param content the template content
     * @throws IllegalArgumentException if prohibited or unsafe HTML elements are detected
     */
    private void validateHtmlSafety(String content) {
        if (!content.contains("<")) {
            return;
        }

        List<String> violations = new ArrayList<>();
        HTML_POLICY.sanitize(content, new HtmlChangeListener<Object>() {
            @Override
            public void discardedTag(Object ctx, String elementName) {
                violations.add("Prohibited HTML tag: <" + elementName + ">");
            }

            @Override
            public void discardedAttributes(Object ctx, String elementName, String... attributeNames) {
                violations.add("Prohibited attribute(s) on <" + elementName + ">: "
                        + String.join(", ", attributeNames));
            }
        }, null);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "Template content contains prohibited script or unsafe HTML elements: " + violations);
        }
    }

    /**
     * Validates that the content is syntactically valid Velocity Template Language (VTL).
     *
     * @param name    the template name
     * @param content the template content
     * @throws IllegalArgumentException if syntax parsing fails
     */
    private void validateVelocitySyntax(String name, String content) {
        try {
            VelocityEngine ve = new VelocityEngine();
            ve.init(Utils.getSecureVelocityProperties(VELOCITY_REPO_NAME));
            StringResourceRepository repo =
                (StringResourceRepository) ve.getApplicationAttribute(VELOCITY_REPO_NAME);
            String templateKey = (name != null ? name : "test") + "_" + System.currentTimeMillis();
            repo.putStringResource(templateKey, content);
            ve.getTemplate(templateKey);
            repo.removeStringResource(templateKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Velocity template syntax: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the template filename conforms to allowed safe characters and does not contain path traversal.
     *
     * @param name the template filename
     * @throws IllegalArgumentException if the name is invalid
     */
    private void validateTemplateName(String name) {
        if (name == null || name.isBlank() || !SAFE_FILENAME_PATTERN.matcher(name).matches() || name.contains("..")) {
            throw new IllegalArgumentException("Invalid template name: " + name);
        }
    }

    /**
     * Logs a formatted warning indicating an unauthorized access attempt by a non-administrator.
     *
     * @param context the DSpace context
     * @param action  the action attempted
     */
    private void logUnauthorizedAttempt(Context context, String action) {
        EPerson currentUser = (context != null) ? context.getCurrentUser() : null;
        String userIdentification = (currentUser != null)
                ? String.format("User '%s' (ID: %s, Email: %s)",
                        currentUser.getName(), currentUser.getID(), currentUser.getEmail())
                : "Anonymous user";
        log.warn(LogHelper.getHeader(context, "unauthorized_access_attempt",
                String.format("Access denied: %s attempted to %s without administrator privileges.",
                        userIdentification, action)));
    }

    /**
     * Reads and parses a template file into an {@link EmailTemplate} domain object.
     *
     * @param file the template file
     * @return parsed EmailTemplate
     * @throws IOException if reading the file fails
     */
    private EmailTemplate parseTemplateFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        EmailTemplate template = new EmailTemplate();
        template.setName(file.getName());
        template.setContent(content);
        template.setLastModified(Instant.ofEpochMilli(file.lastModified()));
        template.setEtag(computeETag(content));

        // Extract subject
        Matcher subjectMatcher = SUBJECT_PATTERN.matcher(content);
        if (subjectMatcher.find()) {
            template.setSubject(subjectMatcher.group(1));
        }

        // Extract mimetype
        Matcher mimeMatcher = MIMETYPE_PATTERN.matcher(content);
        if (mimeMatcher.find()) {
            template.setMimetype(EmailTemplateMimeType.fromString(mimeMatcher.group(1)));
        } else {
            template.setMimetype(EmailTemplateMimeType.TEXT_PLAIN);
        }

        // Extract variables
        template.setVariables(parseVariables(content));

        return template;
    }

    /**
     * Parses variable definitions from comment headers in the template content.
     *
     * @param content the template content
     * @return list of parsed variables ordered by index
     */
    private List<EmailTemplateVariable> parseVariables(String content) {
        Map<Integer, EmailTemplateVariable> variableMap = new HashMap<>();
        String[] lines = content.split("\\R");

        for (String line : lines) {
            if (!line.startsWith("##")) {
                // If line doesn't start with ## and is not whitespace or other VTL directive, stop header parsing
                if (!line.isBlank() && !line.startsWith("#")) {
                    break;
                }
                continue;
            }

            Matcher m = PARAM_PATTERN.matcher(line);
            if (m.matches()) {
                int index = Integer.parseInt(m.group(1));
                String desc = m.group(2).trim();
                variableMap.putIfAbsent(index,
                    new EmailTemplateVariable(index, desc, "${params[" + index + "]}"));
            }
        }

        List<EmailTemplateVariable> variables = new ArrayList<>(variableMap.values());
        variables.sort(Comparator.comparingInt(EmailTemplateVariable::getIndex));
        return variables;
    }
}
