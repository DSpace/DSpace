/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.EmailTemplate;

/**
 * Service interface for managing email templates stored in {@code {dspace.dir}/config/emails/}.
 *
 * <p>Templates are identified by their filename (e.g. "register", "feedback").
 * They are stored as Apache Velocity (VTL) files on the filesystem.</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public interface EmailTemplateService {

    /**
     * Retrieves all email templates from the config directory.
     *
     * @param context the DSpace context
     * @return list of all email templates, ordered alphabetically by name
     * @throws IOException        if an error occurs reading template files
     * @throws SQLException       if a database error occurs during authorization check
     * @throws AuthorizeException if the current user is not authorized
     */
    List<EmailTemplate> findAll(Context context) throws IOException, SQLException, AuthorizeException;

    /**
     * Retrieves a single email template by its name (filename).
     *
     * @param context the DSpace context
     * @param name    the template filename identifier (e.g., "register")
     * @return the template, or {@code null} if not found
     * @throws IOException        if an error occurs reading the template file
     * @throws SQLException       if a database error occurs during authorization check
     * @throws AuthorizeException if the current user is not authorized
     */
    EmailTemplate findByName(Context context, String name) throws IOException, SQLException, AuthorizeException;

    /**
     * Updates the content of an email template.
     * Validates template name (path traversal, symlink escape) and content
     * (VTL syntax, min/max length, XSS sanitization) before saving.
     *
     * @param context the DSpace context
     * @param name    the template filename identifier (e.g., "register")
     * @param content the new template content
     * @return the updated template
     * @throws IOException        if an error occurs writing the template file
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if the current user is not authorized
     */
    EmailTemplate update(Context context, String name, String content)
            throws IOException, SQLException, AuthorizeException;

    /**
     * Computes the ETag for a given template content.
     * The ETag is computed as a SHA-256 hash of the content for optimistic concurrency control.
     *
     * @param content the template content
     * @return the hex-encoded SHA-256 digest
     */
    String computeETag(String content);

    /**
     * Validates template content for minimum/maximum length, XSS safety, and VTL syntax.
     *
     * @param name    the template name
     * @param content the template content to validate
     * @throws IllegalArgumentException if the content fails any validation rule
     */
    void validateContent(String name, String content);
}
