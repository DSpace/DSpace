/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.FilterUtils;
import org.dspace.content.logic.TrueFilter;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * This curation task will register a DOI for an item, optionally ignoring any logical filtering applied
 * to normal identifier registration and DOI service operation.
 *
 * @author Kim Shepherd
 */
public class RegisterDOI extends AbstractCurationTask {
    // Curation task status
    private int status = Curator.CURATE_SUCCESS;
    // The skipFilter boolean has a default value of 'true', as per intended operation
    private boolean skipFilter = true;
    // The distributed boolean has a default value of 'false' for safest operation
    private boolean distributed = false;
    // Prefix for configuration module
    private static final String PLUGIN_PREFIX = "doi-curation";
    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RegisterDOI.class);
    // DOI provider
    private DOIIdentifierProvider provider;
    private Filter trueFilter;

    /**
     * Initialise the curation task and read configuration, instantiate the DOI provider
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        // Get distribution behaviour from configuration, with a default value of 'false'
        distributed = configurationService.getBooleanProperty(PLUGIN_PREFIX + ".distributed", false);
        log.debug("PLUGIN_PREFIX = " + PLUGIN_PREFIX + ", skipFilter = " + skipFilter +
            ", distributed = " + distributed);
        // Instantiate DOI provider singleton
        provider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        trueFilter = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                "always_true_filter", TrueFilter.class);
    }

    /**
     * Override the abstract 'perform' method to either distribute, or perform single-item
     * depending on configuration. By default, the task is *not* distributed, since that could be unsafe
     * and the original purpose of this task is to essentially implement a "Register DOI" button on the Edit Item page.
     * @param dso DSpaceObject for which to register a DOI (must be item)
     * @return status indicator
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // Check distribution configuration
        if (distributed) {
            // This task is configured for distributed use. Call distribute() and let performItem handle
            // the main processing.
            distribute(dso);
        } else {
            // This task is NOT configured for distributed use (default). Instead process a single item directly
            if (dso instanceof Item) {
                Item item = (Item) dso;
                performRegistration(item);
            } else {
                log.warn("DOI registration attempted on non-item DSpace Object: " + dso.getID());
            }
            return status;
        }
        return status;
    }

    /**
     * This is called when the task is distributed (ie. called on a set of items or over a whole structure)
     * @param item the DSpace Item
     */
    @Override
    protected void performItem(Item item) {
        performRegistration(item);
    }

    /**
     * Shared 'perform' code between perform() and performItem() - a curation wrapper for the register() method
     * @param item the item for which to register a DOI
     */
    private void performRegistration(Item item) {
        // Request DOI registration and report results
        String doi = register(item);
        String result = "DOI registration task performed on " + item.getHandle() + ".";
        if (doi != null) {
            result += " DOI: (" + doi + ")";
        } else {
            result += " DOI was null, either item was filtered or an error was encountered.";
        }
        setResult(result);
        report(result);
    }

    /**
     * Perform the DOIIdentifierProvider.register call, with skipFilter passed as per config and defaults
     * @param item The item for which to register a DOI
     */
    private String register(Item item) {
        String doi = null;
        // Attempt DOI registration and report successes and failures
        try {
            Filter filter = FilterUtils.getFilterFromConfiguration("identifiers.submission.filter.curation",
                    trueFilter);
            doi = provider.register(Curator.curationContext(), item, filter);
            if (doi != null) {
                String message = "New DOI minted in database for item " + item.getHandle() + ": " + doi
                    + ". This DOI will be registered online with the DOI provider when the queue is next run";
                report(message);
            } else {
                log.error("Got a null DOI after registering...");
            }
        } catch (SQLException e) {
            // Exception obtaining context
            log.error("Error obtaining curator context: " + e.getMessage());
            status = Curator.CURATE_ERROR;
        } catch (DOIIdentifierNotApplicableException e) {
            // Filter returned 'false' so DOI was not registered. This is normal behaviour when filter is running.
            log.info("Item was filtered from DOI registration: " + e.getMessage());
            String message = "Item " + item.getHandle() + " was skipped from DOI registration because it matched " +
                "the item filter configured in identifier-services.xml.";
            report(message);
            status = Curator.CURATE_SUCCESS;
        } catch (IdentifierException e) {
            // Any other identifier exception is probably a true error
            log.error("Error registering identifier: " + e.getMessage());
            status = Curator.CURATE_ERROR;
        }

        return doi;
    }

}
