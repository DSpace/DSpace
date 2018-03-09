/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.model.BioExternalIdentifier;
import org.dspace.authority.orcid.model.BioName;
import org.dspace.authority.orcid.model.BioResearcherUrl;
import org.dspace.authority.util.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLtoBio extends Converter {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(XMLtoBio.class);

    /**
     * orcid-message XPATHs
     */

    protected String ORCID_BIO = "//orcid-bio";

//    protected String ORCID = "parent::*/orcid";
    protected String ORCID = "parent::*/orcid-identifier/path";

    protected String PERSONAL_DETAILS = "personal-details";
    protected String GIVEN_NAMES = PERSONAL_DETAILS + "/given-names";
    protected String FAMILY_NAME = PERSONAL_DETAILS + "/family-name";
    protected String CREDIT_NAME = PERSONAL_DETAILS + "/credit-name";
    protected String OTHER_NAMES = PERSONAL_DETAILS + "/other-names";
    protected String OTHER_NAME = OTHER_NAMES + "/other-name";

    protected String CONTACT_DETAILS = "contact-details";
    protected String COUNTRY = CONTACT_DETAILS + "/address/country";

    protected String KEYWORDS = "keywords";
    protected String KEYWORD = KEYWORDS + "/keyword";

    protected String EXTERNAL_IDENTIFIERS = "external-identifiers";
    protected String EXTERNAL_IDENTIFIER = EXTERNAL_IDENTIFIERS + "/external-identifier";
    protected String EXTERNAL_ID_ORCID = "external-id-orcid";
    protected String EXTERNAL_ID_COMMNON_NAME = "external-id-common-name";
    protected String EXTERNAL_ID_REFERENCE = "external-id-reference";
    protected String EXTERNAL_ID_URL = "external-id-url";

    protected String RESEARCHER_URLS = "researcher-urls";
    protected String RESEARCHER_URL = "researcher-urls/researcher-url";
    protected String URL_NAME = "url-name";
    protected String URL = "url";

    protected String BIOGRAPHY = ORCID_BIO + "/biography";

    protected String AFFILIATIONS = ORCID_BIO + "/affiliation";

    /**
     * Regex
     */

    protected String ORCID_NOT_FOUND = "ORCID [\\d-]* not found";


    @Override
    public List<Bio> convert(Document xml) {
        List<Bio> result = new ArrayList<Bio>();

        if (XMLErrors.check(xml)) {

            try {
                Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, ORCID_BIO);
                while (iterator.hasNext()) {
                    Bio bio = convertBio(iterator.next());
                    result.add(bio);
                }
            } catch (XPathExpressionException e) {
                log.error("Error in xpath syntax", e);
            }
        } else {
            processError(xml);
        }

        return result;
    }

    private Bio convertBio(Node node) {
        Bio bio = new Bio();

        setOrcid(node,bio);
        setPersonalDetails(node, bio);
        setContactDetails(node, bio);
        setKeywords(node, bio);
        setExternalIdentifiers(node, bio);
        setResearcherUrls(node, bio);
        setBiography(node, bio);

        return bio;
    }

    @Override
    protected void processError(Document xml)  {
        String errorMessage = XMLErrors.getErrorMessage(xml);

        if(errorMessage.matches(ORCID_NOT_FOUND))
        {
            // do something?
        }

        log.error("The orcid-message reports an error: " + errorMessage);
    }


    private void setOrcid(Node node, Bio bio) {
        try {
            String orcid = XMLUtils.getTextContent(node, ORCID);
            bio.setOrcid(orcid);
        } catch (XPathExpressionException e) {
            log.debug("Error in finding the biography in bio xml.", e);
        }
    }

    protected void setBiography(Node xml, Bio bio) {
        try {
            String biography = XMLUtils.getTextContent(xml, BIOGRAPHY);
            bio.setBiography(biography);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the biography in bio xml.", e);
        }
    }

    protected void setResearcherUrls(Node xml, Bio bio) {
        try {
            NodeList researcher_urls = XMLUtils.getNodeList(xml, RESEARCHER_URL);
            if (researcher_urls != null) {
                for (int i = 0; i < researcher_urls.getLength(); i++) {
                    Node researcher_url = researcher_urls.item(i);
                    if (researcher_url.getNodeType() != Node.TEXT_NODE) {
                        String url_name = XMLUtils.getTextContent(researcher_url, URL_NAME);
                        String url = XMLUtils.getTextContent(researcher_url, URL);
                        BioResearcherUrl researcherUrl = new BioResearcherUrl(url_name, url);
                        bio.addResearcherUrl(researcherUrl);
                    }
                }
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the researcher url in bio xml.", e);
        }
    }

    protected void setExternalIdentifiers(Node xml, Bio bio) {
        try {

            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, EXTERNAL_IDENTIFIER);
            while (iterator.hasNext()) {
                Node external_identifier = iterator.next();
                String id_orcid = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_ORCID);
                String id_common_name = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_COMMNON_NAME);
                String id_reference = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_REFERENCE);
                String id_url = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_URL);
                BioExternalIdentifier externalIdentifier = new BioExternalIdentifier(id_orcid, id_common_name, id_reference, id_url);
                bio.addExternalIdentifier(externalIdentifier);
            }

        } catch (XPathExpressionException e) {
            log.error("Error in finding the external identifier in bio xml.", e);
        }
    }

    protected void setKeywords(Node xml, Bio bio) {
        try {
            NodeList keywords = XMLUtils.getNodeList(xml, KEYWORD);
            if (keywords != null) {
                for (int i = 0; i < keywords.getLength(); i++) {
                    String keyword = keywords.item(i).getTextContent();
                    String[] split = keyword.split(",");
                    for (String k : split) {
                        bio.addKeyword(k.trim());
                    }
                }
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the keywords in bio xml.", e);
        }
    }

    protected void setContactDetails(Node xml, Bio bio) {
        try {
            String country = XMLUtils.getTextContent(xml, COUNTRY);
            bio.setCountry(country);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the country in bio xml.", e);
        }
    }

    protected void setPersonalDetails(Node xml, Bio bio) {
        BioName name = bio.getName();

        try {
            String givenNames = XMLUtils.getTextContent(xml, GIVEN_NAMES);
            name.setGivenNames(givenNames);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the given names in bio xml.", e);
        }

        try {
            String familyName = XMLUtils.getTextContent(xml, FAMILY_NAME);
            name.setFamilyName(familyName);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the family name in bio xml.", e);
        }

        try {
            String creditName = XMLUtils.getTextContent(xml, CREDIT_NAME);
            name.setCreditName(creditName);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the credit name in bio xml.", e);
        }

        try {

            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, OTHER_NAME);
            while (iterator.hasNext()) {
                Node otherName = iterator.next();
                String textContent = otherName.getTextContent();
                name.getOtherNames().add(textContent.trim());
            }

        } catch (XPathExpressionException e) {
            log.error("Error in finding the other names in bio xml.", e);
        }
    }

}
