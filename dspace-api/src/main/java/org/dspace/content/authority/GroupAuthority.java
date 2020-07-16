/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class GroupAuthority implements ChoiceAuthority {
    private static final Logger log = Logger.getLogger(GroupAuthority.class);

    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Context context = null;
        if (limit <= 0) {
            limit = 20;
        }
        context = new Context();
        List<Group> groups = null;
        try {
            groups = groupService.search(context, text, start, limit);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        List<Choice> choiceList = new ArrayList<Choice>();
        for (Group group : groups) {
            choiceList.add(new Choice(group.getID().toString(), group.getName(), group.getName()));
        }
        Choice[] results = new Choice[choiceList.size()];
        results = choiceList.toArray(results);
        return new Choices(results, start, groups.size(), Choices.CF_AMBIGUOUS, groups.size() > (start + limit), 0);
    }

    @Override
    public String getLabel(String key, String locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPluginInstanceName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPluginInstanceName(String name) {
        // TODO Auto-generated method stub
    }
}