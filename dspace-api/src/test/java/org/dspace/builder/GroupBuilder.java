/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Builder to construct Group objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class GroupBuilder extends AbstractDSpaceObjectBuilder<Group> {

    private Group group;

    protected GroupBuilder(Context context) {
        super(context);

    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            group = c.reloadEntity(group);
            if (group != null) {
                delete(c, group);
                c.complete();
            }
        }
    }

    public static GroupBuilder createGroup(final Context context) {
        GroupBuilder builder = new GroupBuilder(context);
        return builder.create(context);
    }

    private GroupBuilder create(final Context context) {
        this.context = context;
        try {
            group = groupService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    @Override
    protected DSpaceObjectService<Group> getService() {
        return groupService;
    }

    @Override
    public Group build() {
        try {
            groupService.update(context, group);
        } catch (Exception e) {
            return handleException(e);
        }
        return group;
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

    public static void deleteGroup(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Group group = groupService.find(c, uuid);
            if (group != null) {
                try {
                    groupService.delete(c, group);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
    }

}
