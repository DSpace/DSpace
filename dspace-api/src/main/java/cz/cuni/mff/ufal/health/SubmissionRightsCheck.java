/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package cz.cuni.mff.ufal.health;

import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SubmissionRightsCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        Map<String, Integer> info = new HashMap<>();
        try {
            Context context = new Context();
            ItemIterator it = Item.findAll(context);
            while (it.hasNext()) {
                Item i = it.next();
                Metadatum[] labels = i.getMetadata("dc", "rights", "label",
                        Item.ANY);
                String pub_dc_value = "";

                if (labels.length > 0) {
                    for (Metadatum dc : labels) {
                        if (pub_dc_value.length() == 0) {
                            pub_dc_value = dc.value;
                        } else {
                            pub_dc_value = pub_dc_value + " " + dc.value;
                        }
                    }
                } else {
                    pub_dc_value = "no licence";
                }

                if (!info.containsKey(pub_dc_value)) {
                    info.put(pub_dc_value, 0);
                }
                info.put(pub_dc_value, info.get(pub_dc_value) + 1);
            }
            context.complete();

            for (Map.Entry<String, Integer> e : info.entrySet()) {
                ret += String.format("%s: %s\n", e.getKey(), e.getValue());
            }

        } catch (SQLException e) {
            error(e);
        }
        return ret;
    }

}
