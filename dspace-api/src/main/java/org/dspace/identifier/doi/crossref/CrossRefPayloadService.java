/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi.crossref;

import static org.dspace.identifier.doi.crossref.CrossRefConnector.getTypeText;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.ParameterizedDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.services.ConfigurationService;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * responsibility of this class is to build a payload suitable for uploading to the CrossRef
 * <a href="https://www.crossref.org/documentation/register-maintain-records/direct-deposit-xml/https-post/">API</a>
 */
@Component
public class CrossRefPayloadService {

    private static final Logger LOG = LoggerFactory.getLogger(CrossRefPayloadService.class);

    private final ConfigurationService configurationService;
    private final PluginService pluginService;
    private final String crosswalkName;
    private final String institution;

    @Autowired
    CrossRefPayloadService(PluginService pluginService,
                           ConfigurationService configurationService,
                           @Value("${identifier.doi.crossref.crosswalkname:Crossref}") String crosswalkName) {
        this.pluginService = pluginService;
        this.crosswalkName = crosswalkName;
        this.configurationService = configurationService;

        if (configurationService.hasProperty("identifier.doi.crossref.institution")) {
            this.institution = configurationService.getProperty("identifier.doi.crossref.institution");
        } else {
            this.institution = null;
        }
    }

    Element disseminate(Context context, DSpaceObject dso, String type, String doi) throws DOIIdentifierException {

        var xwalk = lookupXwalk(type);

        Map<String, String> parameters = new HashMap<>();
        if (this.institution != null) {
            parameters.put("institution", this.institution);
        }

        if (!xwalk.canDisseminate(dso)) {
            LOG.error("Crosswalk {} cannot disseminate DSO with type {} and ID {}. Giving up reserving the DOI {}.",
                    this.crosswalkName,
                    dso.getType(),
                    dso.getID(),
                    doi);

            throw new DOIIdentifierException("Cannot disseminate " + getTypeText(dso) + "/" + dso.getID()
                                             + " using crosswalk " + this.crosswalkName + ".",
                    DOIIdentifierException.CONVERSION_ERROR);
        }

        try {
            return xwalk.disseminateElement(context, dso, parameters);
        } catch (CrosswalkException | IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private ParameterizedDisseminationCrosswalk lookupXwalk(String type) {
        var xwalk = (ParameterizedDisseminationCrosswalk) pluginService
                .getNamedPlugin(DisseminationCrosswalk.class,
                        this.crosswalkName + "_" + type.replace(" ", "_").toLowerCase());

        // default crosswalk
        if (xwalk == null) {
            xwalk = (ParameterizedDisseminationCrosswalk) pluginService
                    .getNamedPlugin(DisseminationCrosswalk.class, this.crosswalkName + "_other");
        }

        if (xwalk == null) {
            throw new RuntimeException("Can't find crosswalk '" + crosswalkName
                                       + "_" + type.replace(" ", "_").toLowerCase()
                                       + "' or " + crosswalkName  + "_other!");
        }

        return xwalk;
    }
}
