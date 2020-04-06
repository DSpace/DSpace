package org.dspace.xmlworkflow.cristin;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * <p>Command line script to detect when two cristin IDs are present in the dataset and
 * to output a human readable list of the matches.</p>
 *
 * <p>This works by querying the metadata table in DSpace for the Cristin IDs of items,
 * and building a map of item ids to Cristin IDs, thus determining quickly if there are
 * any duplicates</p>
 *
 * <p><strong>Execution</strong></p>
 *
 * <p>The script can be run from the command line using the dsrun argument of the dspace
 * script, and does not require any further command line arguments.  The output is a
 * human-readable report of those Cristin IDs which are shared by more than one item.</p>
 *
 * <pre>
 *     [dspace]/bin/dspace dsrun no.uio.duo.DeduplicateCristinIds
 * </pre>
 */
public class DeduplicateCristinIds {

    // map to hold the item id to cristin id mapping
    private Map<String, List<UUID>> cristinIds = new HashMap<>();

    // dc value representing the cristin id field
    public MetadataManager.DCValue dcv = null;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(DeduplicateCristinIds.class);

    /**
     * Run this script.  No arguments are required
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)
            throws Exception {
        DeduplicateCristinIds dedupe = new DeduplicateCristinIds();
        Map<String, List<UUID>> duplicates = dedupe.detect();
        String report = dedupe.reportOn(duplicates);
        System.out.println(report);
    }

    /**
     * Take the result map of duplicate items and convert them into something human readable
     *
     * @param duplicates a map of Cristin IDs to DSpace Item IDs
     * @return
     * @throws SQLException
     */
    public String reportOn(Map<String, List<UUID>> duplicates) throws SQLException {
        Context context = new Context();
        StringBuilder sb = new StringBuilder();
        for (String cristinId : duplicates.keySet()) {
            StringBuilder ib = new StringBuilder();
            boolean first = true;
            for (UUID uuid : duplicates.get(cristinId)) {
                String separator = first ? "" : ", ";
                first = false;
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                Item item = itemService.find(context, uuid);
                ib.append(separator).append(getItemHandle(item)).append(" (id: ").append(item.getID()).append(")");
            }
            sb.append("Cristin ID ").append(cristinId).append(" is shared by items: ");
            sb.append(ib);
            sb.append("\n");
        }
        context.abort();
        return sb.toString();
    }

    /**
     * Detect the duplicates in the dataset
     *
     * @return
     * @throws IOException
     */
    public Map<String, List<UUID>> detect() throws IOException, CristinException {
        this.getCristinIdMap();
        return this.extractDuplicates();
    }

    private Map<String, List<UUID>> extractDuplicates() {
        Map<String, List<UUID>> duplicates = new HashMap<>();
        for (String cristinId : this.cristinIds.keySet()) {
            if (this.cristinIds.get(cristinId).size() > 1) {
                duplicates.put(cristinId, this.cristinIds.get(cristinId));
            }
        }
        return duplicates;
    }

    private void getCristinIdMap() throws IOException, CristinException {
        // just mine all of the cristin ids in the database, and record all the ids
        // of the items that have those ids.  This means by the end we will know
        // which cristin ids are associated with which item ids, and it will be easy to
        // test for and report on multiples
        try {
            String cfg = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("cristin", "cristinid.field");
            this.dcv = new MetadataManager().makeMetadatumValue(cfg, null);
            Context context = new Context();
            MetadataSchema schema = ContentServiceFactory.getInstance().getMetadataSchemaService().find(context, "cristin");
            MetadataField metadataField = ContentServiceFactory.getInstance().getMetadataFieldService().findByElement(context, schema, this.dcv.element, this.dcv.qualifier);
            List<MetadataValue> metadataValueList = ContentServiceFactory.getInstance().getMetadataValueService().findByField(context, metadataField);
            for (MetadataValue metadataValue : metadataValueList) {
                UUID uuid = metadataValue.getDSpaceObject().getID();
                String value = metadataValue.getValue();
                if (this.cristinIds.containsKey(value)) {
                    if (!this.cristinIds.get(value).contains(uuid)) {
                        this.cristinIds.get(value).add(uuid);
                    }
                } else {
                    List<UUID> uuids = new ArrayList<>();
                    uuids.add(uuid);
                    this.cristinIds.put(value, uuids);
                }
            }
            context.abort();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Internal utitity method to get a description of the handle
     *
     * @param item The item to get a description of
     * @return The handle, or in workflow
     */
    private static String getItemHandle(Item item) {
        String handle = item.getHandle();
        return (handle != null) ? handle : " in workflow";
    }
}
