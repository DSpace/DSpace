/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.dspace.authority.orcid.model.Profile;
import org.dspace.authority.util.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XMLtoProfile extends XMLtoBio
{
    private static Logger log = Logger.getLogger(XMLtoProfile.class);

    protected String ORCID_PROFILE = "//orcid-profile";
    protected String ORCID_BIO = "orcid-bio";
    protected String ORCID_ACTIVITIES = "orcid-activities";
    protected String AFFILIATION = ORCID_ACTIVITIES + "/affiliations/affiliation[type='employment']/organization/name";

    public List<Profile> convertProfile(Document xml) {
        List<Profile> result = new ArrayList<Profile>();

        if (XMLErrors.check(xml)) {

            try {
                Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, ORCID_PROFILE);
                while (iterator.hasNext()) {
                    Profile profile = convertProfile(iterator.next());
                    result.add(profile);
                }
            } catch (XPathExpressionException e) {
                log.error("Error in xpath syntax", e);
            }
        } else {
            processError(xml);
        }

        return result;
    }

    private Profile convertProfile(Node node) {
        Profile profile = new Profile();

        try {
            Node bioNode = XMLUtils.getNode(node, ORCID_BIO);

            setOrcid(bioNode, profile);
            setPersonalDetails(bioNode, profile);
            setContactDetails(bioNode, profile);
            setKeywords(bioNode, profile);
            setExternalIdentifiers(bioNode, profile);
            setResearcherUrls(bioNode, profile);
            setBiography(bioNode, profile);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the bio in profile xml.", e);
        }

        setAffiliations(node, profile);

        return profile;
    }

    protected void setAffiliations(Node xml, Profile profile) {
        try {
            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, AFFILIATION);
            while (iterator.hasNext()) {
                String affiliation = iterator.next().getTextContent();
                profile.addAffiliation(affiliation);
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the affiliations in profile xml.", e);
        }
    }
}
