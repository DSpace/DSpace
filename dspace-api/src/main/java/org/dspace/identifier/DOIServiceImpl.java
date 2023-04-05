/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.dao.DOIDAO;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the {@link DOI} object.
 * This class is responsible for all business logic calls for the DOI object
 * and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class DOIServiceImpl implements DOIService {

    @Autowired(required = true)
    protected DOIDAO doiDAO;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    private static final Pattern DOI_URL_PATTERN
            = Pattern.compile("http(s)?://([a-z0-9-.]+)?doi.org(?<path>/.*)",
                    Pattern.CASE_INSENSITIVE);
    private static final String DOI_URL_PATTERN_PATH_GROUP = "path";

    private static final String RESOLVER_DEFAULT = "https://doi.org";

    protected DOIServiceImpl() {

    }

    @Override
    public void update(Context context, DOI doi) throws SQLException {
        doiDAO.save(context, doi);
    }

    @Override
    public DOI create(Context context) throws SQLException {
        return doiDAO.create(context, new DOI());
    }

    @Override
    public DOI findByDoi(Context context, String doi) throws SQLException {
        return doiDAO.findByDoi(context, doi);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        return doiDAO.findDOIByDSpaceObject(context, dso);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude)
        throws SQLException {
        return doiDAO.findDOIByDSpaceObject(context, dso, statusToExclude);
    }

    @Override
    public String DOIToExternalForm(String identifier) throws IdentifierException {
        if (null == identifier) {
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        }

        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        }

        String resolver = getResolver();

        if (identifier.startsWith(DOI.SCHEME)) { // doi:something
            StringBuilder result = new StringBuilder(resolver);
            if (!resolver.endsWith("/")) {
                result.append('/');
            }
            result.append(identifier.substring(DOI.SCHEME.length()));
            return result.toString();
        }

        if (identifier.startsWith("10.") && identifier.contains("/")) { // 10.something
            StringBuilder result = new StringBuilder(resolver);
            if (!resolver.endsWith("/")) {
                result.append('/');
            }
            result.append(identifier);
            return result.toString();
        }

        if (identifier.startsWith(resolver + "/10.")) { // https://doi.org/10.something
            return identifier;
        }

        Matcher matcher = DOI_URL_PATTERN.matcher(identifier);
        if (matcher.matches()) { // various old URL forms
            return resolver + matcher.group(DOI_URL_PATTERN_PATH_GROUP);
        }

        throw new IdentifierException(identifier + "does not seem to be a DOI.");
    }

    @Override
    public String DOIFromExternalFormat(String identifier) throws DOIIdentifierException {
        Pattern pattern = Pattern.compile("^" + getResolver() + "/+(10\\..*)$");
        Matcher matcher = pattern.matcher(identifier);
        if (matcher.find()) {
            return DOI.SCHEME + matcher.group(1);
        }

        throw new DOIIdentifierException("Cannot recognize DOI!",
                                         DOIIdentifierException.UNRECOGNIZED);
    }

    @Override
    public String formatIdentifier(String identifier) throws DOIIdentifierException {
        if (null == identifier) {
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        }

        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        }

        if (identifier.startsWith(DOI.SCHEME)) { // doi:something
            return identifier;
        }

        if (identifier.startsWith("10.") && identifier.contains("/")) { // 10.something
            return DOI.SCHEME + identifier;
        }

        String resolver = getResolver();
        if (identifier.startsWith(resolver + "/10.")) { //https://doi.org/10.something
            return DOI.SCHEME + identifier.substring(resolver.length());
        }

        Matcher matcher = DOI_URL_PATTERN.matcher(identifier);
        if (matcher.matches()) { // various old URL forms
            return DOI.SCHEME + matcher.group(DOI_URL_PATTERN_PATH_GROUP).substring(1);
        }

        throw new DOIIdentifierException(identifier + "does not seem to be a DOI.",
                                         DOIIdentifierException.UNRECOGNIZED);
    }

    @Override
    public List<DOI> getDOIsByStatus(Context context, List<Integer> statuses) throws SQLException {
        return doiDAO.findByStatus(context, statuses);
    }

    @Override
    public List<DOI> getSimilarDOIsNotInState(Context context, String doiPattern, List<Integer> statuses,
                                              boolean dsoIsNotNull)
        throws SQLException {
        return doiDAO.findSimilarNotInState(context, doiPattern, statuses, dsoIsNotNull);
    }

    @Override
    public String getResolver() {
        String resolver = configurationService.getProperty("identifier.doi.resolver",
                RESOLVER_DEFAULT);
        if (resolver.endsWith("/")) {
            resolver = resolver.substring(0, resolver.length() - 1);
        }
        return resolver;
    }
}
