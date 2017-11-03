/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Builder to construct Group objects
 */
public class GroupBuilder extends AbstractBuilder<Group> {

    private Group group;

    @Override
    protected DSpaceObjectService<Group> getDsoService() {
        return groupService;
    }

    @Override
    public Group build() {
        return group;
    }

    public GroupBuilder createGroup(final Context context) {
        this.context = context;
        try {
            group = groupService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public GroupBuilder withName(String groupName) {
        try {
            groupService.setName(group, groupName);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public GroupBuilder withParent(Group parent) {
        try {
            groupService.addMember(context, parent, group);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public GroupBuilder addMember(EPerson eperson) {
        try {
            groupService.addMember(context, group, eperson);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }
}
