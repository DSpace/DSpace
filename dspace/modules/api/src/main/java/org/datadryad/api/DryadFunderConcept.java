package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.storage.StorageException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;

import static org.datadryad.api.DryadJournalConcept.PAYMENT_PLAN;
import static org.datadryad.api.DryadJournalConcept.WEBSITE;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadFunderConcept extends DryadOrganizationConcept {
    public static final String FUNDER_ID = "identifier";

    private static Logger log = Logger.getLogger(DryadFunderConcept.class);

    static {
        metadataProperties.setProperty(FUNDER_ID, "funder.identifier");

        defaultMetadataValues.setProperty(metadataProperties.getProperty(FUNDER_ID), "");
    }

    // these are mandatory elements

    public DryadFunderConcept() {
        Context context = null;
        try {
            context = new Context();
            create(context);
            context.commit();
            for (String prop : metadataProperties.stringPropertyNames()) {
                String mdString = metadataProperties.getProperty(prop);
                this.setConceptMetadataValue(mdString, defaultMetadataValues.getProperty(mdString));
            }
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    } // JAXB needs this

    public DryadFunderConcept(Context context, Concept concept) {
        super();
        setUnderlyingConcept(context, concept);
        fullName = getConceptMetadataValue(metadataProperties.getProperty(FULLNAME));
    }

    public DryadFunderConcept(Context context, String fullName) throws StorageException {
        this.setFullName(fullName);
        try {
            context.commit();
        } catch (Exception e) {
            log.error("exception " + e.getMessage());
        }
    }

    @Override
    public void create(Context context) {
        try {
            context.turnOffAuthorisationSystem();
            Scheme funderScheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.dryad_fundingEntity"));
            Concept newConcept = funderScheme.createConcept(context);
            this.setUnderlyingConcept(context, newConcept);
            context.commit();
            context.restoreAuthSystemState();
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
        }
    }

    @JsonIgnore
    public static Boolean conceptIsValidFunder(Concept concept) {
        return ((concept != null) && (concept.getSingleMetadata(metadataProperties.getProperty(FULLNAME)) != null));
    }
}
