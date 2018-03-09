/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.Map;

/**
 * Part of cocoon authentication for resource. Requires that user (eperson) is a member of a specified group.
 *
 * @author Peter Dietz (pdietz84@gmailcom)
 */
public class SpecifiedGroupAuthenticatedSelector implements Selector {
    private static Logger log = Logger.getLogger(SpecifiedGroupAuthenticatedSelector.class);

    private String SPECIFIED_GROUP = "statistics_viewer";

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Override
    public boolean select(String groupName, Map objectModel, Parameters parameters) {
        boolean authorized = false;

        if(groupName.equals("statistics_viewer")) {
            try
            {
                Context context = ContextUtil.obtainContext(objectModel);
                Group statsGroup = groupService.findByName(context, groupName);

                if(statsGroup != null && context.getCurrentUser() != null) {
                    //The Stats Group exists, now lets check that the current user is a member.
                    if(groupService.isMember(context, statsGroup)) {
                        //YES, this person is a member of this group. Let them through.
                        authorized = true;
                    }
                }
            } catch (SQLException e) {
                log.error("SQL Error during stats group lookup.");
            }
        } else {
            log.warn("Pattern/test must be statistics_viewer");
        }

        return authorized;
    }
}
