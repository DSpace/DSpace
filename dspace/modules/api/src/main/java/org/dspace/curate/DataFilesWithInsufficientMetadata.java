package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 *
 * @author Dan Leehr (dan.leehr@nescent.org)
 *
 */
public class DataFilesWithInsufficientMetadata extends ItemsWithInsufficientMetadata {

    /**
       Process a single item.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        // Must have exactly one dc.rights.uri
        DCValue[] references = item.getMetadata("dc.rights.uri");
        if(references.length != 1) {
            String error = "Expected 1 value for dc.rights.uri and found" + references.length;
            DCValue[] doi = item.getMetadata("dc.identifier");
            if (doi.length > 0) {
                report(doi[0].value + ',' + item.getHandle() + ',' + error);
            } else {
                report(',' + item.getHandle() + ',' + error);
            }
        }

        // Must have at least one creator
        // 		match="dim:field[@element='contributor'][not(@qualifier='correspondingAuthor')]">
        DCValue[] creators = item.getMetadata("dc.contributor");
        Integer validCreators = 0;
        for(DCValue value : creators) {
            if("correspondingAuthor".equals(value.qualifier)) {
                continue;
            }
            validCreators++;
        }
        if(validCreators == 0) {
            String error = "No dc.contributor that is not a corresponding author";
            DCValue[] doi = item.getMetadata("dc.identifier");
            if (doi.length > 0) {
                report(doi[0].value + ',' + item.getHandle() + ',' + error);
            } else {
                report(',' + item.getHandle() + ',' + error);
            }
        }
    }
}
