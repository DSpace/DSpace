/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import static java.lang.String.format;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.converter.JsonLdHttpMessageConverter;
import org.dspace.app.ldn.model.Actor;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.model.Object;
import org.dspace.app.ldn.model.Service;
import org.dspace.app.ldn.model.Url;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.app.util.Util;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The service used for requesting to get in touch with 
 * external Review/Endorsement services 
 * 
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 *
 */
@Component
public class OfferReviewService implements BusinessService {

    private final static Logger log = LogManager.getLogger(OfferReviewService.class);

    @Autowired
    private ConfigurationService configurationService;

    private final RestTemplate restTemplate;

    private HandleService handleService;

    private ItemService itemService;

    /**
     * Initialize rest template with appropriate message converters.
     */
    public OfferReviewService() {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new JsonLdHttpMessageConverter());
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
    }

    @Override
    public void doProcessing(Item item) {
        Set<String> endpoints = LDNUtils.getMetadataLdnInitialize(item);
        endpoints.forEach(endpoint -> {

            log.info("Target LDN Inbox URL {}", endpoint);
            log.info("Requesting review for item {}", item.getID());

            String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
            String dspaceUIUrl = configurationService.getProperty("dspace.ui.url");
            String dspaceName = configurationService.getProperty("dspace.name");
            String dspaceLdnInboxUrl = configurationService.getProperty("ldn.notify.local-inbox-endpoint");

            log.info("DSpace Server URL {}", dspaceServerUrl);
            log.info("DSpace UI URL {}", dspaceUIUrl);
            log.info("DSpace Name {}", dspaceName);
            log.info("DSpace LDN Inbox URL {}", dspaceLdnInboxUrl);

            // Build Notification

            Notification notification = new Notification();

            notification.setId(format("urn:uuid:%s", UUID.randomUUID()));
            notification.addType("Offer");
            notification.addType("coar-notify:ReviewAction");

            Actor actor = new Actor();
            actor.setId(dspaceUIUrl);
            actor.setName(dspaceName);
            actor.addType("Service");

            Object object = new Object();
            object.setTitle(item.getName());
            object.setId(format("%s/handle/%s", dspaceUIUrl, item.getID()));
            object.setIetfCiteAs(handleService.getCanonicalForm(item.getHandle()));
            object.addType("sorg:ScholarlyArticle");

            Url url = new Url();
            String bitstreamId = retrieveBitstreamUrl(item);
            url.setId(bitstreamId);
            url.setType(getMetadataType(item));
            url.setMediaType(retrieveMimeTypeFromFilePath(bitstreamId));
            object.setUrl(url);

            Service origin = new Service();
            origin.setId(dspaceUIUrl);
            origin.setInbox(dspaceLdnInboxUrl);
            origin.addType("Service");

            Service target = new Service();
            target.setId(endpoint);
            target.setInbox(endpoint);
            target.addType("Service");

            notification.setActor(actor);
            notification.setObject(object);
            notification.setOrigin(origin);
            notification.setTarget(target);

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Notification> request = new HttpEntity<Notification>(notification, headers);

            log.info("Requesting review {}", request);

            restTemplate.postForLocation(URI.create(target.getInbox()), request);
        });
    }

    @Override
    public String getServiceName() {
        return "Offer:ReviewAction";
    }

    /**
     * Gets the URL to a PDF using a very basic strategy by assuming that the PDF is
     * in the default content bundle, and that the item only has one public
     * bitstream and it is a PDF.
     *
     * @param  item item to get PDF URL from
     * @return      URL that the PDF can be directly downloaded from
     */
    private String retrieveBitstreamUrl(Item item) {
        try {
            Bitstream bitstream = findLinkableFulltext(item);
            if (bitstream != null) {
                StringBuilder path = new StringBuilder();
                path.append(configurationService.getProperty("dspace.ui.url"));

                if (item.getHandle() != null) {
                    path.append("/bitstream/");
                    path.append(item.getHandle());
                    path.append("/");
                    path.append(bitstream.getSequenceID());
                } else {
                    path.append("/retrieve/");
                    path.append(bitstream.getID());
                }

                path.append("/");
                path.append(Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
                return path.toString();
            }
        } catch (UnsupportedEncodingException ex) {
            log.debug(ex.getMessage());
        } catch (SQLException ex) {
            log.debug(ex.getMessage());
        }

        return "";
    }

    /**
     * A bitstream is considered linkable fulltext when it is either
     * <ul>
     * <li>the item's only bitstream (in the ORIGINAL bundle); or</li>
     * <li>the primary bitstream</li>
     * </ul>
     * Additionally, this bitstream must be publicly viewable.
     *
     * @param  item         bitstream's parent item
     * @return              a linkable bitstream or null if none found
     * @throws SQLException if database error
     */
    protected Bitstream findLinkableFulltext(Item item) throws SQLException {
        Bitstream bestSoFar = null;

        List<Bundle> contentBundles = itemService.getBundles(item, "ORIGINAL");
        for (Bundle bundle : contentBundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream candidate : bitstreams) {
                if (candidate.equals(bundle.getPrimaryBitstream())) { // is primary -> use this one
                    if (isPublic(candidate)) {
                        return candidate;
                    }
                } else {
                    if (bestSoFar == null && isPublic(
                        candidate)) { // if bestSoFar is null but the candidate is not public you don't use it and try
                        // to find another
                        bestSoFar = candidate;
                    }
                }
            }
        }

        return bestSoFar;
    }

    /**
     * Find out whether bitstream is readable by the public.
     *
     * @param  bitstream the target bitstream
     * @return           whether bitstream is readable by the Anonymous group
     */
    protected boolean isPublic(Bitstream bitstream) {
        if (bitstream == null) {
            return false;
        }
        boolean result = false;
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();
            result = AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                bitstream, Constants.READ, true);
        } catch (SQLException e) {
            log.error(
                "Cannot determine whether bitstream is public, assuming it isn't. bitstream_id=" + bitstream.getID(),
                e);
        }
        return result;
    }

    /**
     * Retrieves the dc.type metadata
     * 
     * @param item the dspace item 
     * @return Set<String> the set of metadata values as string
     */
    private Set<String> getMetadataType(Item item) {
        List<MetadataValue> metadataValues = item.getMetadata();

        Set<String> typeSet = metadataValues.stream().filter(metadataValue -> {
            return metadataValue.getMetadataField().getMetadataSchema().getName().equals("dc") &&
                metadataValue.getMetadataField().getElement().equals("type");
        }).map(MetadataValue::getValue).collect(Collectors.toSet());

        return typeSet;
    }

    /**
     * Return the mediatype for the given resource
     * 
     * @param stringPath
     * @return String the mediatype for the current 
     */
    public String retrieveMimeTypeFromFilePath(String stringPath) {
        String mimeType = "";
        try {
            Path path = new File(stringPath).toPath();
            mimeType = Files.probeContentType(path);
        } catch (Exception e) {
            log.error(e);
        }
        return mimeType;
    }
}
