/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.script;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.orcid.webhook.OrcidWebhookAction;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Perform a bulk pull from the ORCID registry for all profiles with orcid or
 * with both the orcid and the access token.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidBulkPull extends DSpaceRunnable<OrcidBulkPullScriptConfiguration<OrcidBulkPull>> {

    private List<OrcidWebhookAction> orcidWebhookActions;

    private OrcidSynchronizationService orcidSynchronizationService;

    private ItemService itemService;

    private Context context;

    private boolean onlyLinkedProfiles;

    @Override
    public void setup() throws ParseException {

        orcidWebhookActions = OrcidServiceFactory.getInstance().getOrcidWebhookActions();
        itemService = ContentServiceFactory.getInstance().getItemService();
        orcidSynchronizationService = OrcidServiceFactory.getInstance().getOrcidSynchronizationService();

        if (commandLine.hasOption('l')) {
            onlyLinkedProfiles = true;
        }

    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        try {
            context.turnOffAuthorisationSystem();
            performWebhook();
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performWebhook() {
        Iterator<Item> iterator = orcidSynchronizationService.findProfilesWithOrcid(context);
        int counter = 0;
        while (iterator.hasNext()) {
            Item profile = iterator.next();
            if (!(onlyLinkedProfiles && hasNotAccessToken(profile))) {
                counter++;
                performWebhook(profile);
            }
        }
        handler.logInfo("Processed " + counter + " profiles");
    }

    private void performWebhook(Item profile) {

        String orcid = getOrcid(profile);

        try {
            orcidWebhookActions.forEach(plugin -> plugin.perform(context, profile, orcid));
            handler.logInfo("Processed profile with orcid id " + orcid + " with success");
            itemService.update(context, profile);
        } catch (Exception ex) {
            handler.logError("An error occurs processing profile with orcid id "
                + orcid + ": " + getRootCauseMessage(ex));
        }

    }

    private String getOrcid(Item profile) {
        return itemService.getMetadataFirstValue(profile, "person", "identifier", "orcid", Item.ANY);
    }

    private boolean hasNotAccessToken(Item profile) {
        return itemService.getMetadataFirstValue(profile, "cris", "orcid", "access-token", Item.ANY) == null;
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OrcidBulkPullScriptConfiguration<OrcidBulkPull> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("orcid-bulk-pull",
            OrcidBulkPullScriptConfiguration.class);
    }


}
