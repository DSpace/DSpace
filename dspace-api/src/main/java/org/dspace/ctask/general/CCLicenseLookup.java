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

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.curate.Mutative;
import org.dspace.license.CreativeCommons;
import org.dspace.license.CCLookup;

import static org.dspace.curate.Curator.*;

/**
 * CCLicenseLookup assigns to an item (depending on configuration) the
 * Creative Commons license name, and/or license bitstream, if there is a
 * value assigned to the configured CCLicense URI metadata field of that item.
 * Items lacking a value in URI field are skipped, since CC licenses are optional.
 *
 * @author richardrodgers
 */

@Mutative
@Distributive
public class CCLicenseLookup extends AbstractCurationTask
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(CCLicenseLookup.class);
    // CC Web service interface
    private final CCLookup ccl = new CCLookup();
     // field configured for license URI
    private final CreativeCommons.MdField uriField = CreativeCommons.getCCField("uri");
    // field configured for license name
    private final CreativeCommons.MdField nameField = CreativeCommons.getCCField("name");
    // update configuration flags
    private boolean updateName = false;
    private boolean updateBitstream = false;
     // task status
    private int status = CURATE_UNSET;

    /**
     * Initializes task
     *
     * @param curator  Curator object performing this task
     * @param taskId the configured local name of the task 
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        updateName = taskBooleanProperty("update.name", true);
        updateBitstream = taskBooleanProperty("update.bitstream", true);
    }
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException  {
        distribute(dso);
        return status;
    }

    /**
     * Perform curation on a DSpace Item 
     * 
     * @param item - the DSpace Item
     * @throws IOException, SQLException
     */
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        StringBuilder resultSb = new StringBuilder(safeItemName(item));
        // only attempt lookup if there is a CC License URI value
        String licenseUri = uriField.ccItemValue(item);
        if (licenseUri != null) {
            ccl.issue(licenseUri);
            try {
                if (updateName) {
                    // assume that we always want to synchronize the name to the URI
                    // this means that if a name is present and different, replace it
                    String oldName = nameField.ccItemValue(item);
                    String newName = ccl.getLicenseName();
                    if (oldName == null || ! oldName.equals(newName)) {
                        if (oldName != null) {
                            nameField.removeItemValue(item, oldName);
                            resultSb.append(" removed name: '").append(oldName).append("'");
                        }
                        nameField.addItemValue(item, newName);
                        resultSb.append(" added name: '").append(newName).append("'");
                    }
                }
                if (updateBitstream) {
                    // simliar considerations apply for the license itself
                    String oldLicense = CreativeCommons.getLicenseRDF(item);
                    String newLicense = ccl.getRdf();
                    if (oldLicense == null || ! oldLicense.equals(newLicense)) {
                        if (oldLicense != null) {
                            // remove the whole bundle - this will also wipe out
                            // any other bitstream license representations
                            CreativeCommons.removeLicense(Curator.curationContext(), item);
                            resultSb.append(" removed license bitstream");
                        }
                        CreativeCommons.setLicenseRDF(Curator.curationContext(), item, newLicense);
                        resultSb.append(" added license bitstream");
                    }
                }
                status = CURATE_SUCCESS;
                report(resultSb.toString());
            } catch (AuthorizeException authE) {
                log.error("Authorization error: " + authE.getMessage());
                throw new IOException("Authorization exception");
            }
        } else {
            status = CURATE_SKIP;
            resultSb.append(" no CC License detected - skipped");
        }
        setResult(resultSb.toString());
    }

    private String safeItemName(Item item) {
        String itemId = item.getHandle();
        if (itemId == null) {
            itemId = "Workflow item: " + item.getID();
        }
        return itemId;
    }
}
