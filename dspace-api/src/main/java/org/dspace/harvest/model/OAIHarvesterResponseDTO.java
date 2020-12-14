/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.transform.TransformerException;

import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.dspace.harvest.HarvestingException;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.w3c.dom.NodeList;

/**
 * Model a response coming from a server after a call via
 * {@link OAIHarvesterClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterResponseDTO {

    private final static DOMBuilder DOM_BUILDER = new DOMBuilder();

    private final Document document;

    private final String resumptionToken;

    private final Set<String> errors;

    public OAIHarvesterResponseDTO(Document document, String resumptionToken, Set<String> errors) {
        this.document = document;
        this.resumptionToken = resumptionToken;
        this.errors = errors;
    }

    /**
     * Builds an instance of OAIHarvesterResponseDTO from a {@link HarvesterVerb}.
     *
     * @param  verb the HarvesterVerb related to the harvest
     * @return      the OAIHarvesterResponseDTO instance
     */
    public static OAIHarvesterResponseDTO fromHarvesterVerb(HarvesterVerb verb) {
        Document document = verb.getDocument() != null ? DOM_BUILDER.build(verb.getDocument()) : null;
        String resumptionToken = getResumptionTokenFromVerb(verb);
        Set<String> errors = retrieveErrors(verb);
        return new OAIHarvesterResponseDTO(document, resumptionToken, errors);
    }

    private static String getResumptionTokenFromVerb(HarvesterVerb verb) {
        try {
            return isListRecords(verb) ? ((ListRecords) verb).getResumptionToken() : null;
        } catch (NoSuchFieldException | TransformerException e) {
            throw new HarvestingException(e);
        }
    }

    private static boolean isListRecords(HarvesterVerb harvesterVerb) {
        return harvesterVerb instanceof ListRecords;
    }

    private static Set<String> retrieveErrors(HarvesterVerb verb) {
        NodeList errorNodeList = getErrorsFromHarvesterVerb(verb);

        Set<String> errors = new HashSet<String>();
        if (errorNodeList == null || errorNodeList.getLength() == 0) {
            return errors;
        }

        for (int i = 0; i < errorNodeList.getLength(); i++) {
            errors.add(errorNodeList.item(i).getAttributes().getNamedItem("code").getTextContent());
        }

        return errors;
    }

    private static NodeList getErrorsFromHarvesterVerb(HarvesterVerb verb) {
        try {
            return verb.getErrors();
        } catch (TransformerException e) {
            throw new HarvestingException(e);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Document getDocument() {
        return document;
    }

    public String getResumptionToken() {
        return resumptionToken;
    }

    public Set<String> getErrors() {
        return Collections.unmodifiableSet(errors);
    }

}
