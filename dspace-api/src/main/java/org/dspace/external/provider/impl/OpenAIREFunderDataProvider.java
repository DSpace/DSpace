/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.openaire.jaxb.model.JAXBXMLHandler;
import org.springframework.beans.factory.annotation.Required;

import org.openaire.jaxb.model.Response;
import org.openaire.jaxb.model.Result;
import org.openaire.jaxb.model.Results;

/**
 * This class is the implementation of the ExternalDataProvider interface that
 * will deal with the OpenAIRE API Funders Data lookup
 */
public class OpenAIREFunderDataProvider implements ExternalDataProvider {

    private static Logger log = LogManager.getLogger(OpenAIREFunderDataProvider.class);

    private String sourceIdentifier;
    private String openaireAPIUrl;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Initialize the class
     *
     * @throws java.io.IOException passed through from HTTPclient.
     */
    public void init() throws IOException {

    }

    /**
     * Makes an instance of the OpenAIREFunderDataProvider class based on the
     * provided parameters. This constructor is called through the spring bean
     * initialization
     */
    private OpenAIREFunderDataProvider(String url) {
        this.openaireAPIUrl = url;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        Response response = null;
        try {
            response = getResponse(id);
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(OpenAIREFunderDataProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            java.util.logging.Logger.getLogger(OpenAIREFunderDataProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        ExternalDataObject externalDataObject = convertToExternalDataObject(response);
        return Optional.of(externalDataObject);
    }

    protected ExternalDataObject convertToExternalDataObject(Response response) {
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        Results results = response.getResults();

        Result result = results.getResult();

        String funderName = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getName() != null) {
            funderName = response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getName();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "funderName", null, null, funderName));
        }

        String funderID = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getId() != null) {
            funderID = response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getId();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "funderIdentifier", null, null, funderID));
        }

        String fundingStream = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFundinglevel1().getParent().getFundinglevel0().getDescription() != null) {
            fundingStream = response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFundinglevel1().getParent().getFundinglevel0().getDescription();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "fundingStream", null, null, fundingStream));
        }

        String awardTitle = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getTitle() != null) {
            awardTitle = response.getResults().getResult().getMetadata().getEntity().getProject().getTitle();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "awardTitle", null, null, awardTitle));
        }

        String awardNumber = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getCode() != null) {
            awardNumber = response.getResults().getResult().getMetadata().getEntity().getProject().getCode();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "awardNumber", null, null, awardNumber));
        }

        String fundingItemAcronym = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getAcronym() != null) {
            fundingItemAcronym = response.getResults().getResult().getMetadata().getEntity().getProject().getAcronym();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "fundingItemAcronym", null, null, fundingItemAcronym));
        }

        String funderJuristiction = "";
        if (response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getJurisdiction() != null) {
            funderJuristiction = response.getResults().getResult().getMetadata().getEntity().getProject().getFundingTree().getFunder().getJurisdiction();
            externalDataObject.addMetadata(new MetadataValueDTO("oaire", "funderJuristiction", null, null, funderJuristiction));
        }

        return externalDataObject;
    }

    /**
     * Retrieve a Person object based on a given project grant identifier
     *
     * @param id projectGrantID 
     * @return Respo
     */
    public Response getResponse(String projectID) throws MalformedURLException, JAXBException {
        String url1 = this.openaireAPIUrl + "search/projects?grantID=" + projectID;
        URL url = new URL(url1);
        Response response = JAXBXMLHandler.unmarshal(url);
        return response;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        return null;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        return 1;
    }

    /**
     * Generic setter for the sourceIdentifier
     *
     * @param sourceIdentifier The sourceIdentifier to be set on this
     * OpenAIREFunderDataProvider
     */
    @Required
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the openaireAPIUrl
     *
     * @return the openaireAPIUrl value of this OpenAIREFunderDataProvider
     */
    public String getOpenAIREAPIUrl() {
        return openaireAPIUrl;
    }

    /**
     * Generic setter for the openaireAPIUrl
     *
     * @param openaireAPIUrl The openaireAPIUrl to be set on this
     * OpenAIREFunderDataProvider
     */
    @Required
    public void getOpenAIREAPIUrl(String openaireAPIUrl) {
        this.openaireAPIUrl = openaireAPIUrl;
    }
}
