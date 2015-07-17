/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package org.dspace.health;


import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        Map<String, Integer> info = new HashMap<String, Integer>();
        try {
            Context context = new Context();
            EPerson[] epersons = EPerson.findAll(context, EPerson.LASTNAME);
            info.put("Count", epersons.length);
            info.put("Can log in (password)", 0);
            info.put("Have email", 0);
            info.put("Have 1st name", 0);
            info.put("Have 2nd name", 0);
            info.put("Have lang", 0);
            info.put("Have netid", 0);
            info.put("Self registered", 0);

            for (EPerson e : epersons) {
                if (e.getEmail() != null && e.getEmail().length() > 0)
                    info.put("Have email", info.get("Have email") + 1);
                if (e.canLogIn())
                    info.put("Can log in (password)",
                        info.get("Can log in (password)") + 1);
                if (e.getFirstName() != null && e.getFirstName().length() > 0)
                    info.put("Have 1st name", info.get("Have 1st name") + 1);
                if (e.getLastName() != null && e.getLastName().length() > 0)
                    info.put("Have 2nd name", info.get("Have 2nd name") + 1);
                if (e.getLanguage() != null && e.getLanguage().length() > 0)
                    info.put("Have lang", info.get("Have lang") + 1);
                if (e.getNetid() != null && e.getNetid().length() > 0)
                    info.put("Have netid", info.get("Have netid") + 1);
                if (e.getNetid() != null && e.getNetid().length() > 0)
                    info.put("Self registered", info.get("Self registered") + 1);
            }
            context.complete();

        } catch (SQLException e) {
            error(e);
        }

        ret += String.format(
            "Users: %d\n", info.get("Count"));
        ret += String.format(
            "Have email: %d\n", info.get("Have email"));
        for (Map.Entry<String, Integer> e : info.entrySet()) {
            if (!e.getKey().equals("Count") && !e.getKey().equals("Have email")) {
                ret += String.format("%s: %s\n", e.getKey(),
                    String.valueOf(e.getValue()));
            }
        }

        try {
            // empty group
            List<String> egs = Core.getEmptyGroups();
            ret += String.format(
                "  Empty groups: #%d\n    %s\n",
                egs.size(), StringUtils.join(egs, ",\n    "));

            List<Integer> subs = Core.getSubscribers();
            ret += String.format(
                "Subscribers: #%d [%s]\n",
                subs.size(), StringUtils.join(subs, ", "));

            subs = Core.getSubscribedCollections();
            ret += String.format(
                "Subscribed cols.: #%d [%s]\n",
                subs.size(), StringUtils.join(subs, ", "));

        } catch (SQLException e) {
            error(e);
        }

        return ret;
    }
}
