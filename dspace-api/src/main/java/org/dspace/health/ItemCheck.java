/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;


import org.dspace.core.Context;

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
        Context context = new Context();
        Core core = new Core(context);
        try {
            for (Map.Entry<String, Integer> name_count : core.getCommunities()) {
                ret += String.format("Community [%s]: %d\n",
                    name_count.getKey(), name_count.getValue());
                tot_cnt += name_count.getValue();
            }
        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += "\nCollection sizes:\n";
            ret += core.getCollectionSizesInfo();
        } catch (SQLException e) {
            error(e);
        }

        ret += String.format(
            "\nPublished items (archived, not withdrawn): %d\n", tot_cnt);
        try {
            ret += String.format(
                "Withdrawn items: %d\n", core.getWithdrawnItemsCount());
            ret += String.format(
                "Not published items (in workspace or workflow mode): %d\n",
                core.getNotArchivedItemsCount());

            for (Object[] row : core.getWorkspaceItemsRows()) {
                ret += String.format("\tIn Stage %s: %s\n",
                    row[0],// "stage_reached"),
                    row[1]// "cnt")
                );
            }

            ret += String.format(
                "\tWaiting for approval (workflow items): %d\n",
                core.getWorkflowItemsCount());

        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += core.getObjectSizesInfo();
            context.complete();
        } catch (SQLException e) {
            error(e);
        }
        return ret;
    }
}
