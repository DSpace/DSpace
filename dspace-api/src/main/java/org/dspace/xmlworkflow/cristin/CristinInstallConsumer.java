/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.embargo.factory.EmbargoServiceFactory;
import org.dspace.embargo.service.EmbargoService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * <p>Event consumer for Duo which responds to Item Installs.</p>
 *
 * <p>This consumer will first determine if an item has embargo metadata attached to it.  If not
 * it will apply the default access policies to the item as per the Duo requirements.  To do this
 * it delegates to the DuoPolicyManager.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>To enable the install consumer, in dspace.cfg we need to add the name of the consumer
 * ("duo") to the list of available consumers, and specify the class name (this class) and
 * the conditions on which it will be triggered:</p>
 *
 * <pre>
 * event.dispatcher.default.consumers = search, browse, eperson, cristin
 * event.consumer.cristin.class = org.dspace.xmlworkflow.cristin.CristinInstallConsumer
 * event.consumer.cristin.filters = Item+Install
 * </pre>
 */
public class CristinInstallConsumer implements Consumer {
    /**
     * Initialise the install consumer.  Does nothing.
     *
     * @throws Exception
     */
    public void initialize() throws Exception {
    }

    /**
     * End the consumer.  Does nothing.
     *
     * @param context
     * @throws Exception
     */
    public void end(Context context) throws Exception {
    }

    /**
     * Finish the consumer.  Does nothing.
     *
     * @param context
     * @throws Exception
     */
    public void finish(Context context) throws Exception {
    }

    /**
     * Consume an Install event.  If the item is being installed and it is not
     * embargoed then we want to set the item's policies.  See {@link CristinPolicyManager}
     * for details
     *
     * @param context
     * @param event
     * @throws Exception
     */
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        // we mustn't set the policies if the item is embargoed; these will have already
        // been removed, and should stay that way
        if (this.isEmbargoed(context, item)) {
            return;
        }
        CristinPolicyManager dpm = new CristinPolicyManager();
        dpm.setDefaultPolicies(context, item);
    }

    private boolean isEmbargoed(Context context, Item item) throws Exception {
        // if an embargo field isn't configured, then the item can't be embargoed
        String liftDateField = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("embargo.field.lift");
        if (liftDateField == null) {
            return false;
        }

        // if there is no embargo value, the item isn't embargoed
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<MetadataValue> embargoes = itemService.getMetadataByMetadataString(item, liftDateField);
        if (embargoes.size() == 0) {
            return false;
        }

        // if the embargo date is in the past, then the item isn't embargoed
        try {
            EmbargoService embargoService = EmbargoServiceFactory.getInstance().getEmbargoService();
            DCDate embargoDate = embargoService.getEmbargoTermsAsDate(context, item);
            if ((new Date()).getTime() > embargoDate.toDate().getTime()) {
                return false;
            }
        } catch (SQLException e) {
            throw new Exception(e);
        } catch (AuthorizeException e) {
            throw new Exception(e);
        }

        return true;
    }

}
