/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static java.lang.String.format;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.ldn.factory.NotifyServiceFactory;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LDN;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.web.ContextUtil;

/**
 * class for creating a new LDN Messages of installed item
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageConsumer implements Consumer {

    private NotifyPatternToTriggerService notifyPatternToTriggerService;
    private NotifyServiceInboundPatternService inboundPatternService;
    private LDNMessageService ldnMessageService;
    private ConfigurationService configurationService;
    private ItemService itemService;
    private BitstreamService bitstreamService;
    private final String RESUBMISSION_SUFFIX = "-resubmission";
    private final String ENDORSEMENT_PATTERN = "request-endorsement";
    private final String REVIEW_PATTERN = "request-review";

    @Override
    public void initialize() throws Exception {
        notifyPatternToTriggerService = NotifyServiceFactory.getInstance().getNotifyPatternToTriggerService();
        ldnMessageService = NotifyServiceFactory.getInstance().getLDNMessageService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        inboundPatternService = NotifyServiceFactory.getInstance().getNotifyServiceInboundPatternService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        if (event.getSubjectType() != Constants.ITEM ||
            event.getEventType() != Event.INSTALL) {
            return;
        }

        Item item = (Item) event.getSubject(context);
        if (item == null) {
            return;
        }
        createManualLDNMessages(context, item);
        createAutomaticLDNMessages(context, item);
    }

    private void createManualLDNMessages(Context context, Item item) throws SQLException, JsonProcessingException {
        List<NotifyPatternToTrigger> patternsToTrigger =
            notifyPatternToTriggerService.findByItem(context, item);
        // Note that multiple patterns can be submitted and not all support resubmission
        // 1. Extract all patterns that accept resubmissions, i.e. endorsement and review
        List<Integer> patternsSupportingResubmission = patternsToTrigger.stream()
                .filter(p -> p.getPattern().equals(REVIEW_PATTERN) || p.getPattern().equals(ENDORSEMENT_PATTERN))
                .map(NotifyPatternToTrigger::getID).toList();

        String resubmissionReplyToID = null;

        for (NotifyPatternToTrigger patternToTrigger : patternsToTrigger) {
            // Only try to fetch resubmission ID if the pattern support resubmission
            if (patternsSupportingResubmission.contains(patternToTrigger.getID())) {
                resubmissionReplyToID = findResubmissionReplyToUUID(context, item, patternToTrigger.getNotifyService());
            }

            createLDNMessage(context,patternToTrigger.getItem(),
                    patternToTrigger.getNotifyService(),
                    patternToTrigger.getPattern(),
                    resubmissionReplyToID);
        }
    }

    private void createAutomaticLDNMessages(Context context, Item item) throws SQLException, JsonProcessingException {

        List<NotifyServiceInboundPattern> inboundPatterns = inboundPatternService.findAutomaticPatterns(context);

        for (NotifyServiceInboundPattern inboundPattern : inboundPatterns) {
            if (StringUtils.isEmpty(inboundPattern.getConstraint()) ||
                evaluateFilter(context, item, inboundPattern.getConstraint())) {
                createLDNMessage(context, item, inboundPattern.getNotifyService(),
                        inboundPattern.getPattern(), null);
            }
        }
    }

    private String findResubmissionReplyToUUID(Context context, Item item, NotifyServiceEntity service)
            throws SQLException {
        // 1.1 Check whether this is a new version submission
        VersionHistoryService versionHistoryService = VersionServiceFactory.getInstance()
                .getVersionHistoryService();
        VersionHistory versionHistory = versionHistoryService.findByItem(context, item);

        if (versionHistory != null) {
            Version currentVersion = versionHistoryService.getVersion(context, versionHistory, item);
            Version previousVersion = versionHistoryService.getPrevious(context, versionHistory, currentVersion);
            if (previousVersion != null) {
                // 1.2 and a TentativeReject notification, matching the current pattern's service, was received for the
                // previous item version
                return ldnMessageService.findEndorsementOrReviewResubmissionIdByItem(context,
                        previousVersion.getItem(), service);
            }
        }
        // New submission (new item, or previous version with a tentativeReject notification not found)
        return null;
    }

    private boolean evaluateFilter(Context context, Item item, String constraint) {
        LogicalStatement filter =
            new DSpace().getServiceManager().getServiceByName(constraint, LogicalStatement.class);

        return filter != null && filter.getResult(context, item);
    }

    private void createLDNMessage(Context context, Item item, NotifyServiceEntity service, String pattern,
                                  String resubmissionID)
            throws SQLException, JsonProcessingException {
        // Amend current pattern name to trigger
        // Endorsement or Review offer resubmissions: append '-resubmission' to pattern name to choose the correct
        // LDN message template: e.g. request-endorsement-resubmission or request-review-resubmission
        LDN ldn = (resubmissionID != null)
                ? getLDNMessage(pattern + RESUBMISSION_SUFFIX) : getLDNMessage(pattern);
        LDNMessageEntity ldnMessage =
                ldnMessageService.create(context, format("urn:uuid:%s", UUID.randomUUID()));

        ldnMessage.setObject(item);
        ldnMessage.setTarget(service);
        ldnMessage.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED);
        ldnMessage.setQueueTimeout(new Date());

        String actorID = null;
        boolean serviceUsesActorEmailId =
                configurationService.getBooleanProperty(
                        String.format("ldn.notification.supportsActorEmailId.%d", service.getID()), false);
        if (serviceUsesActorEmailId) {
            // If the service has been configured to use actorEmailId, we use the submitter's email and name
            if (item.getSubmitter() != null) {
                actorID = item.getSubmitter().getEmail();
            } else {
                // Use configured fallback email (defaults to mail.admin property)
                actorID = configurationService.getProperty("ldn.notification.email.submitter.fallback");
            }
        }
        appendGeneratedMessage(ldn,
                ldnMessage,
                actorID,
                (actorID != null && item.getSubmitter() != null) ? item.getSubmitter().getFullName() : null,
                resubmissionID);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);
        ldnMessage.setType(StringUtils.joinWith(",", notification.getType()));

        ArrayList<String> notificationTypeArrayList = new ArrayList<String>(notification.getType());
        // sorting the list
        Collections.sort(notificationTypeArrayList);
        ldnMessage.setActivityStreamType(notificationTypeArrayList.get(0));
        ldnMessage.setCoarNotifyType(notificationTypeArrayList.get(1));
        // If a resubmission, set inReplyTo
        if (resubmissionID != null) {
            ldnMessage.setInReplyTo(ldnMessageService.find(context, resubmissionID));
        }

        ldnMessageService.update(context, ldnMessage);
    }

    private LDN getLDNMessage(String pattern) {
        try {
            return LDN.getLDNMessage(I18nUtil.getLDNFilename(Locale.getDefault(), pattern));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void appendGeneratedMessage(LDN ldn, LDNMessageEntity ldnMessage, String actorID, String actorName,
                                        String resubmissionId) {
        Item item = (Item) ldnMessage.getObject();
        if (actorID != null) {
            ldn.addArgument("mailto:" + actorID);
        } else {
            ldn.addArgument(getUiUrl());
        }
        ldn.addArgument(configurationService.getProperty("ldn.notify.inbox"));
        ldn.addArgument(actorName != null ? actorName : configurationService.getProperty("dspace.name"));
        ldn.addArgument(Objects.requireNonNullElse(ldnMessage.getTarget().getUrl(), ""));
        ldn.addArgument(Objects.requireNonNullElse(ldnMessage.getTarget().getLdnUrl(), ""));
        ldn.addArgument(getUiUrl() + "/handle/" + ldnMessage.getObject().getHandle());
        ldn.addArgument(getIdentifierUri(item));
        ldn.addArgument(generateBitstreamDownloadUrl(item));
        ldn.addArgument(getBitstreamMimeType(findPrimaryBitstream(item)));
        ldn.addArgument(ldnMessage.getID());
        ldn.addArgument(getRelationUri(item));
        ldn.addArgument("http://purl.org/vocab/frbr/core#supplement");
        ldn.addArgument(format("urn:uuid:%s", UUID.randomUUID()));
        if (actorID != null) {
            ldn.addArgument("Person");
        } else {
            ldn.addArgument("Service");
        }
        // Param 14: UI URL, LDN message origin
        ldn.addArgument(getUiUrl());
        // Param 15: inReplyTo ID, used in endorsement resubmission notifications
        if (resubmissionId != null) {
            ldn.addArgument(String.format("\"inReplyTo\": \"%s\",", resubmissionId));
        }

        ldnMessage.setMessage(ldn.generateLDNMessage());
    }

    private String getUiUrl() {
        return configurationService.getProperty("dspace.ui.url");
    }

    private String getIdentifierUri(Item item) {
        return itemService.getMetadataByMetadataString(item, "dc.identifier.uri")
                          .stream()
                          .findFirst()
                          .map(MetadataValue::getValue)
                          .orElse("");
    }

    private String getRelationUri(Item item) {
        String relationMetadata = configurationService.getProperty("ldn.notify.relation.metadata", "dc.relation");
        return itemService.getMetadataByMetadataString(item, relationMetadata)
                          .stream()
                          .findFirst()
                          .map(MetadataValue::getValue)
                          .orElse("");
    }

    private String generateBitstreamDownloadUrl(Item item) {
        String uiUrl = getUiUrl();
        return findPrimaryBitstream(item)
            .map(bs -> uiUrl + "/bitstreams/" + bs.getID() + "/download")
            .orElse("");
    }

    private Optional<Bitstream> findPrimaryBitstream(Item item) {
        List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        return bundles.stream()
                      .findFirst()
                      .map(Bundle::getPrimaryBitstream)
                      .or(() -> bundles.stream()
                                       .findFirst()
                                       .flatMap(bundle -> CollectionUtils.isNotEmpty(bundle.getBitstreams())
                                           ? Optional.of(bundle.getBitstreams().get(0))
                                           : Optional.empty()));
    }

    private String getBitstreamMimeType(Optional<Bitstream> bitstream) {
        return bitstream.map(bs -> {
            try {
                Context context = ContextUtil.obtainCurrentRequestContext();
                BitstreamFormat bitstreamFormat = bs.getFormat(context);
                if (bitstreamFormat.getShortDescription().equals("Unknown")) {
                    return getUserFormatMimeType(bs);
                }
                return bitstreamFormat.getMIMEType();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).orElse("");
    }

    private String getUserFormatMimeType(Bitstream bitstream) {
        return bitstreamService.getMetadataFirstValue(bitstream,
            "dc", "format", "mimetype", Item.ANY);
    }

    @Override
    public void end(Context ctx) throws Exception {

    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

}
