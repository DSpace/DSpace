/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir.entitlement;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.rest.RESTConnector;
import org.dspace.authority.util.XMLUtils;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.fileaccess.factory.FileAccessServiceFactory;
import org.dspace.fileaccess.service.ItemMetadataService;
import org.dspace.importer.external.scidir.util.GeneralUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.Set;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 02 Oct 2015
 */
public abstract class CheckItem {

    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(CheckItem.class);
    protected Document response = null;
    protected Set<String> trueValues = Collections.singleton("true");
    protected Set<String> falseValues = Collections.singleton("false");
    protected boolean useApiKey = true;

    protected static ItemMetadataService itemMetadataService = FileAccessServiceFactory.getInstance().getItemMetadataService();

    /**
     * Sends a rest request to the elsevier api
     *
     * @param item The item to be checked
     * @return  true    when the response element is contained in getTrueValues()
     *          false   when the response element is contained in getFalseValues()
     *          null    when none of the above
     */
    public ArticleAccess check(Item item) {
        ArticleAccess articleAccess = new ArticleAccess();

        String url = ConfigurationManager.getProperty("elsevier-sciencedirect", getUrlConfigKey());
        RESTConnector connect = new RESTConnector(url);

        String pii = itemMetadataService.getPII(item);
        String doi = itemMetadataService.getDOI(item);

        if (StringUtils.isNotBlank(pii)) {
            response = connect.get("hostingpermission/pii/" + pii + getQueryString());

        } else if (StringUtils.isNotBlank(doi)) {
            response = connect.get("hostingpermission/doi/" + doi + getQueryString());
        }

        if (response != null) {
            try {
                Node hostingNode = XMLUtils.getNode(response, getCheckNodeXPath());

                Node hostingAllowedNode = XMLUtils.getNode(hostingNode, "hosting-platform[@type='non-commercial']/document-version[journal_article_version/text()='AM']/hosting-allowed");

                if (hostingAllowedNode != null) {
                    NamedNodeMap attributes = hostingAllowedNode.getAttributes();

                    articleAccess.setAudience(attributes.getNamedItem("audience").getTextContent());
                    articleAccess.setStartDate(attributes.getNamedItem("start_date").getTextContent());
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            }
        }

        if(StringUtils.isBlank(articleAccess.getAudience())){
            if (StringUtils.isNotBlank(pii)) {
            response = connect.get("pii/" + pii + getQueryString());
                log.error("using the fallback access check implementation for article with pii " + pii);

        } else if (StringUtils.isNotBlank(doi)) {
            response = connect.get("doi/" + doi + getQueryString());
                log.error("using the fallback access check implementation for article with doi " + doi);
        }

        if (response != null) {
            try {
                Node node = XMLUtils.getNode(response, getCheckNodeXPath());

                if (node != null) {
                        articleAccess.setAudience(node.getTextContent());
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            }
        }
        }

        // if no permission can be found, set default to restricted
        if(StringUtils.isBlank(articleAccess.getAudience())){
            articleAccess.setAudience("restricted");

            if (StringUtils.isNotBlank(pii)) {
                log.error("fallback found no permissions for article with pii " + pii);

            } else if (StringUtils.isNotBlank(doi)) {
                log.error("fallback found no permissions for article with doi " + doi);
            }
        }

        return articleAccess;
    }


    /**
     * Initializes when check(Item) is called.
     *
     * @return The link, if any, associated with the last item that has been checked.
     */
    public String getLink() {
        String link = null;
        if (response != null) {
            try {
                Node node = XMLUtils.getNode(response, getLinkNodeXPath());
                if (node != null) {
                    link = node.getTextContent();
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            }
        }
        return link;
    }

    private String getQueryString() {
        String key = "apiKey=" + ConfigurationManager.getProperty("elsevier-sciencedirect", "api.key");
        String accept = "httpAccept=text/xml";

        String params;
        if (useApiKey) {
            params = GeneralUtils.join("&", key, accept);
        } else {
            params = accept;
        }
        if (StringUtils.isNotBlank(params)) {
            params = "?" + params;
        }

        return params;
    }

    protected Set<String> getFalseValues() {
        return falseValues;
    }

    protected Set<String> getTrueValues(){
        return trueValues;
    }

    public void setFalseValues(Set<String> falseValues) {
        this.falseValues = falseValues;
    }

    public void setTrueValues(Set<String> trueValues) {
        this.trueValues = trueValues;
    }

    public boolean isUseApiKey() {
        return useApiKey;
    }

    public void setUseApiKey(boolean useApiKey) {
        this.useApiKey = useApiKey;
    }

    protected abstract String getLinkNodeXPath();

    protected abstract String getCheckNodeXPath();

    protected abstract String getUrlConfigKey();

}
