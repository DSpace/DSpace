/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.openaire.jaxb.helper.FundingHelper;
import eu.openaire.jaxb.helper.ProjectHelper;
import eu.openaire.jaxb.model.Response;
import eu.openaire.jaxb.model.Result;
import eu.openaire.oaf.model.base.FunderType;
import eu.openaire.oaf.model.base.FundingTreeType;
import eu.openaire.oaf.model.base.FundingType;
import eu.openaire.oaf.model.base.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.OpenAIRERestConnector;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is the implementation of the ExternalDataProvider interface that
 * will deal with the OpenAIRE External Data lookup
 * 
 * @author paulo-graca
 */
public class OpenAIREFundingDataProvider extends AbstractExternalDataProvider {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(OpenAIREFundingDataProvider.class);

    /**
     * GrantAgreement prefix
     */
    protected static final String PREFIX = "info:eu-repo/grantAgreement";

    private static final String TITLE = "dcTitle";
    private static final String SUBJECT = "dcSubject";
    private static final String AWARD_URI = "awardURI";
    private static final String FUNDER_NAME = "funderName";
    private static final String SPATIAL = "coverageSpatial";
    private static final String AWARD_NUMBER = "awardNumber";
    private static final String FUNDER_ID = "funderIdentifier";
    private static final String FUNDING_STREAM = "fundingStream";
    private static final String TITLE_ALTERNATIVE = "titleAlternative";

    /**
     * rows default limit
     */
    protected static final int LIMIT_DEFAULT = 10;

    /**
     * Source identifier (defined in beans)
     */
    protected String sourceIdentifier;

    /**
     * Connector to handle token and requests
     */
    protected OpenAIRERestConnector connector;

    protected Map<String, MetadataFieldConfig> metadataFields;

    public void init() throws IOException {}

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        // we use base64 encoding in order to use slashes / and other
        // characters that must be escaped for the <:entry-id>
        String decodedId = new String(Base64.getDecoder().decode(id));
        if (!isValidProjectURI(decodedId)) {
            log.error("Invalid ID for OpenAIREFunding - " + id);
            return Optional.empty();
        }
        Response response = searchByProjectURI(decodedId);

        try {
            if (response.getHeader() != null && Integer.parseInt(response.getHeader().getTotal()) > 0) {
                Project project = response.getResults().getResult().get(0).getMetadata().getEntity().getProject();
                ExternalDataObject externalDataObject = new OpenAIREFundingDataProvider
                        .ExternalDataObjectBuilder(project)
                        .setId(generateProjectURI(project))
                        .setSource(sourceIdentifier)
                        .build();
                return Optional.of(externalDataObject);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid Total from response - " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {

        // ensure we have a positive > 0 limit
        if (limit < 1) {
            limit = LIMIT_DEFAULT;
        }

        // OpenAIRE uses pages and first page starts with 1
        int page = (start / limit) + 1;

        // escaping query
        String encodedQuery = encodeValue(query);

        Response projectResponse = connector.searchProjectByKeywords(page, limit, encodedQuery);

        if (projectResponse == null || projectResponse.getResults() == null) {
            return Collections.emptyList();
        }

        List<Project> projects = new ArrayList<Project>();
        for (Result result : projectResponse.getResults().getResult()) {

            if (result.getMetadata() != null && result.getMetadata().getEntity() != null
                    && result.getMetadata().getEntity().getProject() != null) {
                projects.add(result.getMetadata().getEntity().getProject());
            } else {
                throw new IllegalStateException("No project found");
            }
        }

        if (projects.size() > 0) {
            return projects.stream()
                    .map(project -> new OpenAIREFundingDataProvider
                            .ExternalDataObjectBuilder(project)
                            .setId(generateProjectURI(project))
                            .setSource(sourceIdentifier)
                            .build())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        // escaping query
        String encodedQuery = encodeValue(query);

        Response projectResponse = connector.searchProjectByKeywords(0, 0, encodedQuery);
        return Integer.parseInt(projectResponse.getHeader().getTotal());
    }

    /**
     * Generic setter for the sourceIdentifier
     * 
     * @param sourceIdentifier The sourceIdentifier to be set on this
     *                         OpenAIREFunderDataProvider
     */
    @Autowired(required = true)
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public OpenAIRERestConnector getConnector() {
        return connector;
    }

    /**
     * Generic setter for OpenAIRERestConnector
     * 
     * @param connector
     */
    @Autowired(required = true)
    public void setConnector(OpenAIRERestConnector connector) {
        this.connector = connector;
    }

    /**
     * 
     * @param projectURI from type
     *                   info:eu-repo/grantAgreement/FCT/3599-PPCDT/82130/PT
     * @return Response
     */
    public Response searchByProjectURI(String projectURI) {
        String[] splittedURI = projectURI.replaceAll(PREFIX, "").split("/");
        return connector.searchProjectByIDAndFunder(splittedURI[3], splittedURI[1], 1, 1);
    }

    /**
     * Validates if the project has the correct format
     * 
     * @param projectURI
     * @return true if the URI is valid
     */
    private static boolean isValidProjectURI(String projectURI) {
        return Pattern.matches(PREFIX + "/.+/.+/.*", projectURI);
    }

    /**
     * This method returns an URI based on OpenAIRE 3.0 guidelines
     * https://guidelines.openaire.eu/en/latest/literature/field_projectid.html that
     * can be used as an ID if is there any missing part, that part it will be
     * replaced by the character '+'
     * 
     * @param project
     * @return String with an URI like: info:eu-repo/grantAgreement/EC/FP7/244909
     */
    private static String generateProjectURI(Project project) {
        ProjectHelper projectHelper = new ProjectHelper(project.getCodeOrTitleOrAcronym());

        String prefix = PREFIX;
        String funderShortName = "+";
        String fundingName = "+";
        String code = "+";
        String jurisdiction = "+";

        Optional<FundingTreeType> fundingTree = projectHelper.getFundingTreeTypes().stream().findFirst();
        if (!fundingTree.isEmpty()) {
            if (fundingTree.get().getFunder() != null) {
                if (fundingTree.get().getFunder().getShortname() != null) {
                    funderShortName = encodeValue(fundingTree.get().getFunder().getShortname());
                }
                if (fundingTree.get().getFunder().getJurisdiction() != null) {
                    jurisdiction = encodeValue(fundingTree.get().getFunder().getJurisdiction());
                }
            }
            FundingHelper fundingHelper = new FundingHelper(
                    fundingTree.get().getFundingLevel2OrFundingLevel1OrFundingLevel0());
            Optional<FundingType> funding = fundingHelper.getFirstAvailableFunding().stream().findFirst();

            if (!funding.isEmpty()) {
                fundingName = encodeValue(funding.get().getName());
            }

        }

        Optional<String> optCode = projectHelper.getCodes().stream().findFirst();
        if (!optCode.isEmpty()) {
            code = encodeValue(optCode.get());
        }

        return String.format("%s/%s/%s/%s/%s", prefix, funderShortName, fundingName, code, jurisdiction);
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    public Map<String, MetadataFieldConfig> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(Map<String, MetadataFieldConfig> metadataFields) {
        this.metadataFields = metadataFields;
    }

    /**
     * OpenAIRE Funding External Data Builder Class
     * 
     * @author pgraca
     */
    public class ExternalDataObjectBuilder {

        private ExternalDataObject externalDataObject;

        public ExternalDataObjectBuilder(Project project) {
            String funderIdPrefix = "urn:openaire:";
            this.externalDataObject = new ExternalDataObject();

            ProjectHelper projectHelper = new ProjectHelper(project.getCodeOrTitleOrAcronym());
            for (FundingTreeType fundingTree : projectHelper.getFundingTreeTypes()) {
                FunderType funder = fundingTree.getFunder();
                // Funder name
                this.addMetadata(metadataFields.get(FUNDER_NAME), funder.getName());
                // Funder Id - convert it to an urn
                this.addMetadata(metadataFields.get(FUNDER_ID), funderIdPrefix + funder.getId());
                // Jurisdiction
                this.addMetadata(metadataFields.get(SPATIAL), funder.getJurisdiction());

                FundingHelper fundingHelper = new FundingHelper(
                              fundingTree.getFundingLevel2OrFundingLevel1OrFundingLevel0());

                // Funding description
                for (FundingType funding : fundingHelper.getFirstAvailableFunding()) {
                    this.addMetadata(metadataFields.get(FUNDING_STREAM), funding.getDescription());
                }
            }

            // Title
            for (String title : projectHelper.getTitles()) {
                this.addMetadata(metadataFields.get(TITLE), title);
                this.setDisplayValue(title);
                this.setValue(title);
            }
            // Code
            for (String code : projectHelper.getCodes()) {
                this.addMetadata(metadataFields.get(AWARD_NUMBER), code);
            }
            // Website url
            for (String url : projectHelper.getWebsiteUrls()) {
                this.addMetadata(metadataFields.get(AWARD_URI), url);
            }
            // Acronyms
            for (String acronym : projectHelper.getAcronyms()) {
                this.addMetadata(metadataFields.get(TITLE_ALTERNATIVE), acronym);
            }
            // Keywords
            for (String keyword : projectHelper.getKeywords()) {
                this.addMetadata(metadataFields.get(SUBJECT), keyword);
            }
        }

        /**
         * Set the external data source
         * 
         * @param source
         * @return ExternalDataObjectBuilder
         */
        public ExternalDataObjectBuilder setSource(String source) {
            this.externalDataObject.setSource(source);
            return this;
        }

        /**
         * Set the external data display name
         * 
         * @param displayName
         * @return ExternalDataObjectBuilder
         */
        public ExternalDataObjectBuilder setDisplayValue(String displayName) {
            this.externalDataObject.setDisplayValue(displayName);
            return this;
        }

        /**
         * Set the external data value
         * 
         * @param value
         * @return ExternalDataObjectBuilder
         */
        public ExternalDataObjectBuilder setValue(String value) {
            this.externalDataObject.setValue(value);
            return this;
        }

        /**
         * Set the external data id
         * 
         * @param id
         * @return ExternalDataObjectBuilder
         */
        public ExternalDataObjectBuilder setId(String id) {
            // we use base64 encoding in order to use slashes / and other
            // characters that must be escaped for the <:entry-id>
            String base64Id = Base64.getEncoder().encodeToString(id.getBytes());
            this.externalDataObject.setId(base64Id);
            return this;
        }

        public ExternalDataObjectBuilder addMetadata(MetadataFieldConfig metadataField, String value) {
            this.externalDataObject.addMetadata(new MetadataValueDTO(metadataField.getSchema(),
                                                                     metadataField.getElement(),
                                                                     metadataField.getQualifier(), null, value));
            return this;
        }

        /**
         * Build the External Data
         * 
         * @return ExternalDataObject
         */
        public ExternalDataObject build() {
            return this.externalDataObject;
        }
    }

}