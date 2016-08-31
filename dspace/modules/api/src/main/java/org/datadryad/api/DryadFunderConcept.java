package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.datadryad.api.DryadJournalConcept.PAYMENT_PLAN;
import static org.datadryad.api.DryadJournalConcept.WEBSITE;
import org.dspace.content.Item;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadFunderConcept extends DryadOrganizationConcept {
    public static final String FUNDER_ID = "identifier";
    public static final String PREFERRED_LABEL = "fullname";
    public static final String ALT_LABEL = "altLabel";
    public static final String COUNTRY = "country";


    private static Logger log = Logger.getLogger(DryadFunderConcept.class);

    static {
        metadataProperties.setProperty(FUNDER_ID, "funder.identifier");
        metadataProperties.setProperty(ALT_LABEL, "funder.altLabel");
        metadataProperties.setProperty(COUNTRY, "funder.country");

        defaultMetadataValues.setProperty(metadataProperties.getProperty(FUNDER_ID), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(ALT_LABEL), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(COUNTRY), "");
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

    public String getFunderId() {
        return getConceptMetadataValue(metadataProperties.getProperty(FUNDER_ID));
    }

    public void setFunderId(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(FUNDER_ID), value);
    }

    public String getAltLabel() {
        return getConceptMetadataValue(metadataProperties.getProperty(FUNDER_ID));
    }

    public void setAltLabel(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(ALT_LABEL), value);
    }

    public void addAltLabel(String value) {
        addConceptMetadataValue(metadataProperties.getProperty(ALT_LABEL), value);
    }

    public String getCountry() {
        return getConceptMetadataValue(metadataProperties.getProperty(COUNTRY));
    }

    public void setCountry(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(COUNTRY), value);
    }

    @JsonIgnore
    public static Boolean conceptIsValidFunder(Concept concept) {
        return ((concept != null) && (concept.getSingleMetadata(metadataProperties.getProperty(FUNDER_ID)) != null));
    }

    public static DryadFunderConcept getFunderConceptMatchingFunderID(Context context, String funderID) {
        DryadFunderConcept funderConcept = null;
        Concept[] concepts = Concept.searchByMetadata(context, metadataProperties.getProperty(FUNDER_ID), funderID);
        if (concepts.length > 0) {
            funderConcept = new DryadFunderConcept(context, concepts[0]);
        }
        return funderConcept;
    }

    // In order to get all of the information into the fundingEntity string, the authority value will be the FundRef ID,
    // while the value will be in the format "<grant number>@<funder name> (<country>)".

    public static DCValue createFundingEntityMetadata(DryadFunderConcept funderConcept, String grantNumber, int confidence) {
        DCValue result = new DCValue();
        result.schema = "dryad";
        result.element = "fundingEntity";
        result.value = grantNumber + "@" + funderConcept.getFullName() + " (" + funderConcept.getCountry() + ")";
        result.authority = funderConcept.getFunderId();
        result.confidence = confidence;

        return result;
    }

    public static String getFunderNameFromFundingEntity(DCValue fundingMetadata) {
        Matcher matcher = Pattern.compile("(.*?)@(.+)\\s*(\\(.*\\))").matcher(fundingMetadata.value);
        String result = null;
        if (matcher.find()) {
            result = matcher.group(2);
        }

        return result;
    }

    public static String getCountryFromFundingEntity(DCValue fundingMetadata) {
        Matcher matcher = Pattern.compile("(.*?)@(.+)\\s*(\\(.*\\))").matcher(fundingMetadata.value);
        String result = null;
        if (matcher.find()) {
            result = matcher.group(3);
        }

        return result;
    }

    public static String getGrantNumberFromFundingEntity(DCValue fundingMetadata) {
        Matcher matcher = Pattern.compile("(.*?)@(.+)\\s*(\\(.*\\))").matcher(fundingMetadata.value);
        String result = null;
        if (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }
}
