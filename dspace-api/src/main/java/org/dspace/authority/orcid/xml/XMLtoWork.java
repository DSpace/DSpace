/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.dspace.authority.orcid.model.*;
import org.dspace.authority.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLtoWork extends Converter {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(XMLtoWork.class);

    /**
     * orcid-message XPATHs
     */

    protected String ORCID_WORKS = "//orcid-works";
    protected String ORCID_WORK = ORCID_WORKS + "/orcid-work";

    protected String WORK_TITLE = "work-title";
    protected String TITLE = WORK_TITLE + "/title";
    protected String SUBTITLE = WORK_TITLE + "/subtitle";
    protected String TRANSLATED_TITLES = WORK_TITLE + "/translated-title";
    protected String TRANSLATED_TITLES_LANGUAGE = "@language-code";

    protected String SHORT_DESCRIPTION = "short-description";

    protected String WORK_CITATION = "work-citation";
    protected String CITATION_TYPE = WORK_CITATION + "/work-citation-type";
    protected String CITATION = WORK_CITATION + "/citation";

    protected String WORK_TYPE = "work-type";

    protected String PUBLICATION_DATE = "publication-date";
    protected String YEAR = PUBLICATION_DATE + "/year";
    protected String MONTH = PUBLICATION_DATE + "/month";
    protected String DAY = PUBLICATION_DATE + "/day";

    protected String WORK_EXTERNAL_IDENTIFIERS = "work-external-identifiers";
    protected String WORK_EXTERNAL_IDENTIFIER = WORK_EXTERNAL_IDENTIFIERS + "/work-external-identifier";
    protected String WORK_EXTERNAL_IDENTIFIER_TYPE = "work-external-identifier-type";
    protected String WORK_EXTERNAL_IDENTIFIER_ID = "work-external-identifier-id";

    protected String URL = "url";

    protected String WORK_CONTRIBUTOR = "work-contributors";
    protected String CONTRIBUTOR = WORK_CONTRIBUTOR+"/contributor";
    protected String CONTRIBUTOR_ORCID = "contributor-orcid";
    protected String CREDIT_NAME = "credit-name";
    protected String CONTRIBUTOR_EMAIL = "contributor-email";
    protected String CONTRIBUTOR_ATTRIBUTES = "contributor-attributes";
    protected String CONTRIBUTOR_SEQUENCE = "contributor-sequence";
    protected String CONTRIBUTOR_ROLE = "contributor-role";

    protected String WORK_SOURCE = "work-source";


    @Override
    public List<Work> convert(Document document) {
        List<Work> result = new ArrayList<Work>();

        if (XMLErrors.check(document)) {

            try {
                Iterator<Node> iterator = XMLUtils.getNodeListIterator(document, ORCID_WORK);
                while (iterator.hasNext()) {
                    Work work = convertWork(iterator.next());
                    result.add(work);
                }
            } catch (XPathExpressionException e) {
                log.error("Error in xpath syntax", e);
            }
        } else {
            processError(document);
        }

        return result;
    }

    protected Work convertWork(Node node) throws XPathExpressionException {
        Work work = new Work();
        setTitle(node, work);
        setDescription(node, work);
        setCitation(node, work);
        setWorkType(node, work);
        setPublicationDate(node, work);
        setExternalIdentifiers(node, work);
        setUrl(node, work);
        setContributors(node, work);
        setWorkSource(node, work);

        return work;
    }

    protected void setWorkSource(Node node, Work work) throws XPathExpressionException {
        String workSource = XMLUtils.getTextContent(node, WORK_SOURCE);
        work.setWorkSource(workSource);
    }

    protected void setContributors(Node node, Work work) throws XPathExpressionException {

        Set<Contributor> contributors = new HashSet<Contributor>();

        Iterator<Node> iterator = XMLUtils.getNodeListIterator(node, CONTRIBUTOR);
        while (iterator.hasNext()) {
            Node nextContributorNode = iterator.next();
            String orcid = XMLUtils.getTextContent(nextContributorNode, CONTRIBUTOR_ORCID);
            String creditName = XMLUtils.getTextContent(nextContributorNode, CREDIT_NAME);
            String email = XMLUtils.getTextContent(nextContributorNode, CONTRIBUTOR_EMAIL);

            Set<ContributorAttribute> contributorAttributes = new HashSet<ContributorAttribute>();
            NodeList attributeNodes = XMLUtils.getNodeList(nextContributorNode, CONTRIBUTOR_ATTRIBUTES);
            Iterator<Node> attributesIterator = XMLUtils.getNodeListIterator(attributeNodes);
            while (attributesIterator.hasNext()) {
                Node nextAttribute = attributesIterator.next();

                String roleText = XMLUtils.getTextContent(nextAttribute, CONTRIBUTOR_ROLE);
                ContributorAttributeRole role = EnumUtils.lookup(ContributorAttributeRole.class, roleText);

                String sequenceText = XMLUtils.getTextContent(nextAttribute, CONTRIBUTOR_SEQUENCE);
                ContributorAttributeSequence sequence = EnumUtils.lookup(ContributorAttributeSequence.class, sequenceText);

                ContributorAttribute attribute = new ContributorAttribute(role, sequence);
                contributorAttributes.add(attribute);
            }

            Contributor contributor = new Contributor(orcid, creditName, email, contributorAttributes);
            contributors.add(contributor);
        }

        work.setContributors(contributors);
    }

    protected void setUrl(Node node, Work work) throws XPathExpressionException {
        String url = XMLUtils.getTextContent(node, URL);
        work.setUrl(url);
    }

    protected void setExternalIdentifiers(Node node, Work work) throws XPathExpressionException {

        Iterator<Node> iterator = XMLUtils.getNodeListIterator(node, WORK_EXTERNAL_IDENTIFIER);
        while (iterator.hasNext()) {
            Node work_external_identifier = iterator.next();
            String typeText = XMLUtils.getTextContent(work_external_identifier, WORK_EXTERNAL_IDENTIFIER_TYPE);

            WorkExternalIdentifierType type = EnumUtils.lookup(WorkExternalIdentifierType.class, typeText);

            String id = XMLUtils.getTextContent(work_external_identifier, WORK_EXTERNAL_IDENTIFIER_ID);

            WorkExternalIdentifier externalID = new WorkExternalIdentifier(type, id);
            work.setWorkExternalIdentifier(externalID);
        }
    }

    protected void setPublicationDate(Node node, Work work) throws XPathExpressionException {

        String year = XMLUtils.getTextContent(node, YEAR);
        String month = XMLUtils.getTextContent(node, MONTH);
        String day = XMLUtils.getTextContent(node, DAY);

        String publicationDate = year;
        if (StringUtils.isNotBlank(month)) {
            publicationDate += "-" + month;
            if (StringUtils.isNotBlank(day)) {
                publicationDate += "-" + day;
            }
        }

        work.setPublicationDate(publicationDate);
    }

    protected void setWorkType(Node node, Work work) throws XPathExpressionException {

        String workTypeText = XMLUtils.getTextContent(node, WORK_TYPE);
        WorkType workType = EnumUtils.lookup(WorkType.class, workTypeText);

        work.setWorkType(workType);
    }

    protected void setCitation(Node node, Work work) throws XPathExpressionException {

        String typeText = XMLUtils.getTextContent(node, CITATION_TYPE);
        CitationType type = EnumUtils.lookup(CitationType.class, typeText);

        String citationtext = XMLUtils.getTextContent(node, CITATION);

        Citation citation = new Citation(type, citationtext);
        work.setCitation(citation);
    }

    protected void setDescription(Node node, Work work) throws XPathExpressionException {

        String description = null;
        description = XMLUtils.getTextContent(node, SHORT_DESCRIPTION);
        work.setDescription(description);
    }

    protected void setTitle(Node node, Work work) throws XPathExpressionException {

        String title = XMLUtils.getTextContent(node, TITLE);

        String subtitle = XMLUtils.getTextContent(node, SUBTITLE);

        Map<String, String> translatedTitles = new HashMap<String, String>();
        NodeList nodeList = XMLUtils.getNodeList(node, TRANSLATED_TITLES);
        Iterator<Node> iterator = XMLUtils.getNodeListIterator(nodeList);
        while (iterator.hasNext()) {
            Node languageNode = iterator.next();
            String language = XMLUtils.getTextContent(languageNode, TRANSLATED_TITLES_LANGUAGE);
            String translated_title = XMLUtils.getTextContent(languageNode, ".");
            translatedTitles.put(language, translated_title);
        }

        WorkTitle workTitle = new WorkTitle(title, subtitle, translatedTitles);
        work.setWorkTitle(workTitle);
    }

}
