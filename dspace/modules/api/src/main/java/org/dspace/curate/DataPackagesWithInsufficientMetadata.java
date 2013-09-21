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
public class DataPackagesWithInsufficientMetadata extends ItemsWithInsufficientMetadata {

    /**
       Process a single item.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        // Need to have a dc.references
        //  		match="dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi') or starts-with(., 'http://dx.doi')]">

        DCValue[] references = item.getMetadata("dc.relation.isreferencedby");
        Integer validReferences = 0;
        for(DCValue value : references) {
            if(value.value.startsWith("doi") || value.value.startsWith("http://dx.doi")) {
                validReferences++;
            }
        }
        if(validReferences == 0) {
            String error = "no DOI value for dc.relation.isreferencedby";
            DCValue[] doi = item.getMetadata("dc.identifier");
            if (doi.length > 0) {
                report(doi[0].value + ',' + item.getHandle() + ',' + error);
            } else {
                report(',' + item.getHandle() + ',' + error);
            }
        }
    }
}
