/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;


import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author LINDAT/CLARIN dev team
 */
public class ItemCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        int tot_cnt = 0;
        try {
            for (Map.Entry<String, Integer> name_count : Core.getCommunities()) {
                ret += String.format("Community [%s]: %d\n",
                    name_count.getKey(), name_count.getValue());
                tot_cnt += name_count.getValue();
            }
        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += "\nCollection sizes:\n";
            ret += Core.getCollectionSizesInfo();
        } catch (SQLException e) {
            error(e);
        }

        ret += String.format(
            "\nPublished items (archived, not withdrawn): %d\n", tot_cnt);
        try {
            ret += String.format(
                "Withdrawn items: %d\n", Core.getWithdrawnItemsCount());
            ret += String.format(
                "Not published items (in workspace or workflow mode): %d\n",
                Core.getNotArchivedItemsCount());

            for (TableRow row : Core.getWorkspaceItemsRows()) {
                ret += String.format("\tIn Stage %s: %s\n",
                    row.getIntColumn("stage_reached"),
                    row.getLongColumn("cnt"));
            }

            ret += String.format(
                "\tWaiting for approval (workflow items): %d\n",
                Core.getWorkflowItemsCount());

        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += Core.getObjectSizesInfo();
        } catch (SQLException e) {
            error(e);
        }
        return ret;
    }
}
