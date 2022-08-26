/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.dspace.harvest.OAIHarvester.OAI_ADDRESS_ERROR;
import static org.dspace.harvest.OAIHarvester.OAI_DMD_ERROR;
import static org.dspace.harvest.OAIHarvester.OAI_ORE_ERROR;
import static org.dspace.harvest.OAIHarvester.OAI_SET_ERROR;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListIdentifiers;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.harvest.dao.HarvestedCollectionDAO;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.DOMBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

/**
 * Service implementation for the HarvestedCollection object.
 * This class is responsible for all business logic calls for the HarvestedCollection object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedCollectionServiceImpl implements HarvestedCollectionService {

    private static final Namespace ORE_NS = Namespace.getNamespace("http://www.openarchives.org/ore/terms/");
    private static final Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    @Autowired(required = true)
    protected HarvestedCollectionDAO harvestedCollectionDAO;

    protected HarvestedCollectionServiceImpl() {
    }

    @Override
    public HarvestedCollection find(Context context, Collection collection) throws SQLException {
        return harvestedCollectionDAO.findByCollection(context, collection);
    }

    @Override
    public HarvestedCollection create(Context context, Collection collection) throws SQLException {
        HarvestedCollection harvestedCollection = harvestedCollectionDAO.create(context, new HarvestedCollection());
        harvestedCollection.setCollection(collection);
        harvestedCollection.setHarvestType(HarvestedCollection.TYPE_NONE);
        update(context, harvestedCollection);
        return harvestedCollection;
    }

    @Override
    public boolean isHarvestable(Context context, Collection collection) throws SQLException {
        HarvestedCollection hc = find(context, collection);
        if (hc != null && hc.getHarvestType() > 0 && hc.getOaiSource() != null && hc.getOaiSetId() != null &&
            hc.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isHarvestable(HarvestedCollection harvestedCollection) throws SQLException {
        if (harvestedCollection.getHarvestType() > 0 && harvestedCollection
            .getOaiSource() != null && harvestedCollection.getOaiSetId() != null &&
            harvestedCollection.getHarvestStatus() != HarvestedCollection.STATUS_UNKNOWN_ERROR) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isReady(Context context, Collection collection) throws SQLException {
        HarvestedCollection hc = find(context, collection);
        return isReady(hc);
    }

    @Override
    public boolean isReady(HarvestedCollection harvestedCollection) throws SQLException {
        if (isHarvestable(harvestedCollection) && (harvestedCollection
            .getHarvestStatus() == HarvestedCollection.STATUS_READY || harvestedCollection
            .getHarvestStatus() == HarvestedCollection.STATUS_OAI_ERROR)) {
            return true;
        }

        return false;
    }

    @Override
    public List<HarvestedCollection> findAll(Context context) throws SQLException {
        return harvestedCollectionDAO.findAll(context, HarvestedCollection.class);
    }

    @Override
    public List<HarvestedCollection> findReady(Context context) throws SQLException {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        int harvestInterval = configurationService.getIntProperty("oai.harvester.harvestFrequency");
        if (harvestInterval == 0) {
            harvestInterval = 720;
        }

        int expirationInterval = configurationService.getIntProperty("oai.harvester.threadTimeout");
        if (expirationInterval == 0) {
            expirationInterval = 24;
        }

        Date startTime;
        Date expirationTime;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, -1 * harvestInterval);
        startTime = calendar.getTime();

        calendar.setTime(startTime);
        calendar.add(Calendar.HOUR, -2 * expirationInterval);
        expirationTime = calendar.getTime();

        int[] statuses = new int[] {HarvestedCollection.STATUS_READY, HarvestedCollection.STATUS_OAI_ERROR};
        return harvestedCollectionDAO
            .findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(context, startTime,
                                                                               HarvestedCollection.TYPE_NONE, statuses,
                                                                               HarvestedCollection.STATUS_BUSY,
                                                                               expirationTime);
    }

    @Override
    public List<HarvestedCollection> findByStatus(Context context, int status) throws SQLException {
        return harvestedCollectionDAO.findByStatus(context, status);
    }

    @Override
    public HarvestedCollection findOldestHarvest(Context context) throws SQLException {
        return harvestedCollectionDAO
            .findByStatusAndMinimalTypeOrderByLastHarvestedAsc(context, HarvestedCollection.STATUS_READY,
                                                               HarvestedCollection.TYPE_NONE, 1);
    }

    @Override
    public HarvestedCollection findNewestHarvest(Context context) throws SQLException {
        return harvestedCollectionDAO
            .findByStatusAndMinimalTypeOrderByLastHarvestedDesc(context, HarvestedCollection.STATUS_READY,
                                                                HarvestedCollection.TYPE_NONE, 1);
    }

    @Override
    public void delete(Context context, HarvestedCollection harvestedCollection) throws SQLException {
        harvestedCollectionDAO.delete(context, harvestedCollection);
    }

    @Override
    public void update(Context context, HarvestedCollection harvestedCollection) throws SQLException {
        harvestedCollectionDAO.save(context, harvestedCollection);
    }

    @Override
    public boolean exists(Context context) throws SQLException {
        return 0 < harvestedCollectionDAO.count(context);
    }

    /**
     * Verify the existence of an OAI server with the specified set and
     * supporting the provided metadata formats.
     *
     * @param oaiSource  the address of the OAI-PMH provider
     * @param oaiSetId   OAI set identifier
     * @param metaPrefix OAI metadataPrefix
     * @param testORE    whether the method should also check the PMH provider for ORE support
     * @return list of errors encountered during verification. Empty list indicates a "success" condition.
     */
    @Override
    public List<String> verifyOAIharvester(String oaiSource,
                                                  String oaiSetId, String metaPrefix, boolean testORE) {
        List<String> errorSet = new ArrayList<>();

        // First, see if we can contact the target server at all.
        try {
            new Identify(oaiSource);
        } catch (IOException | ParserConfigurationException | TransformerException | SAXException ex) {
            errorSet.add(OAI_ADDRESS_ERROR + ": OAI server could not be reached.");
            return errorSet;
        }

        // Next, make sure the metadata we need is supported by the target server
        Namespace DMD_NS = OAIHarvester.getDMDNamespace(metaPrefix);
        if (null == DMD_NS) {
            errorSet.add(OAI_DMD_ERROR + ":  " + metaPrefix);
            return errorSet;
        }

        String OREOAIPrefix = null;
        String DMDOAIPrefix = null;

        try {
            OREOAIPrefix = OAIHarvester.oaiResolveNamespaceToPrefix(oaiSource, OAIHarvester.getORENamespace().getURI());
            DMDOAIPrefix = OAIHarvester.oaiResolveNamespaceToPrefix(oaiSource, DMD_NS.getURI());
        } catch (IOException | ParserConfigurationException | TransformerException | SAXException ex) {
            errorSet.add(OAI_ADDRESS_ERROR
                + ": OAI did not respond to ListMetadataFormats query  ("
                + ORE_NS.getPrefix() + ":" + OREOAIPrefix + " ; "
                + DMD_NS.getPrefix() + ":" + DMDOAIPrefix + "):  "
                + ex.getMessage());
            return errorSet;
        }

        if (testORE && OREOAIPrefix == null) {
            errorSet.add(OAI_ORE_ERROR + ": The OAI server does not support ORE dissemination");
        }
        if (DMDOAIPrefix == null) {
            errorSet.add(OAI_DMD_ERROR + ": The OAI server does not support dissemination in this format");
        }

        // Now scan the sets and make sure the one supplied is in the list
        boolean foundSet = false;
        try {
            //If we do not want to harvest from one set, then skip this.
            if (!"all".equals(oaiSetId)) {
                ListIdentifiers ls = new ListIdentifiers(oaiSource, null, null, oaiSetId, DMDOAIPrefix);

                // The only error we can really get here is "noSetHierarchy"
                if (ls.getErrors() != null && ls.getErrors().getLength() > 0) {
                    for (int i = 0; i < ls.getErrors().getLength(); i++) {
                        String errorCode = ls.getErrors().item(i).getAttributes().getNamedItem("code").getTextContent();
                        errorSet.add(
                            OAI_SET_ERROR + ": The OAI server does not have a set with the specified setSpec (" +
                                errorCode + ")");
                    }
                } else {
                    // Drilling down to /OAI-PMH/ListSets/set
                    DOMBuilder db = new DOMBuilder();
                    Document reply = db.build(ls.getDocument());
                    Element root = reply.getRootElement();
                    //Check if we can find items, if so this indicates that we have children and our sets exist
                    foundSet = 0 < root.getChild("ListIdentifiers", OAI_NS).getChildren().size();

                    if (!foundSet) {
                        errorSet.add(OAI_SET_ERROR + ": The OAI server does not have a set with the specified setSpec");
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | TransformerException | DOMException | SAXException e) {
            errorSet.add(OAI_ADDRESS_ERROR + ": OAI server could not be reached");
            return errorSet;
        } catch (RuntimeException re) {
            throw re;
        }

        return errorSet;
    }


}
