/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

/**
 * Servlet for editing groups
 * 
 * @author dstuve
 * @version $Revision$
 */
public class GroupEditServlet extends DSpaceServlet
{
	private GroupService groupService;
	
	private EPersonService personService;
	
	@Override
	public void init() throws ServletException {
		super.init();
		groupService = EPersonServiceFactory.getInstance().getGroupService();
		personService = EPersonServiceFactory.getInstance().getEPersonService();
	}
	
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSPost(c, request, response);
    }

    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Find out if there's a group parameter
        UUID groupID = UIUtil.getUUIDParameter(request, "group_id");
        Group group = null;

        if (groupID != null)
        {
            group = groupService.find(c, groupID);
        }

        // group is set
        if (group != null)
        {
            // is this user authorized to edit this group?
            authorizeService.authorizeAction(c, group, Constants.ADD);

            boolean submit_edit = (request.getParameter("submit_edit") != null);
            boolean submit_group_update = (request.getParameter("submit_group_update") != null);
            boolean submit_group_delete = (request.getParameter("submit_group_delete") != null);
            boolean submit_confirm_delete = (request.getParameter("submit_confirm_delete") != null);
            boolean submit_cancel_delete = (request.getParameter("submit_cancel_delete") != null);


            // just chosen a group to edit - get group and pass it to
            // group-edit.jsp
            if (submit_edit && !submit_group_update && !submit_group_delete)
            {
                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
            }
            // update the members of the group
            else if (submit_group_update)
            {
                // first off, did we change the group name?
                String newName = request.getParameter("group_name");

                if (!newName.equals(group.getName()))
                {
                    group.setName(c, newName);
                    groupService.update(c, group);
                }

                List<UUID> eperson_ids = UIUtil.getUUIDParameters(request,
                        "eperson_id");
                List<UUID> group_ids = UIUtil.getUUIDParameters(request, "group_ids");

                // now get members, and add new ones and remove missing ones
                List<EPerson> members = group.getMembers();
                List<Group> membergroups = group.getMemberGroups();

                if (eperson_ids != null)
                {
                    // some epeople were listed, now make group's epeople match
                    // given epeople
                    Set<UUID> memberSet = new HashSet<UUID>();
                    Set<UUID> epersonIDSet = new HashSet<UUID>();

                    // add all members to a set
                    for (EPerson m :  members)
                    {
                        memberSet.add(m.getID());
                    }

                    // now all eperson_ids are put in a set
                    for (UUID e : eperson_ids)
                    {
                        epersonIDSet.add(e);
                    }

                    // process eperson_ids, adding those to group not already
                    // members
                    for (UUID uid : epersonIDSet)
                    {
                        if (!memberSet.contains(uid))
                        {
                            groupService.addMember(c, group, personService.find(c, uid));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (EPerson e : members)
                    {
                        if (!epersonIDSet.contains(e.getID()))
                        {
                            groupService.removeMember(c, group, e);
                        }
                    }
                }
                else
                {
                    // no members found (ids == null), remove them all!

                    for (EPerson e : members)
                    {
                        groupService.removeMember(c, group, e);
                    }
                }

                if (group_ids != null)
                {
                    // some groups were listed, now make group's member groups
                    // match given group IDs
                    Set<UUID> memberSet = new HashSet<UUID>();
                    Set<UUID> groupIDSet = new HashSet<UUID>();

                    // add all members to a set
                    for (Group g : membergroups)
                    {
                        memberSet.add(g.getID());
                    }

                    // now all eperson_ids are put in a set
                    for (UUID uid :  group_ids)
                    {
                        groupIDSet.add(uid);
                    }

                    // process group_ids, adding those to group not already
                    // members
                    for (UUID guid : groupIDSet)
                    {
                        if (!memberSet.contains(guid))
                        {
                            groupService
                                    .addMember(c, group, groupService.find(c, guid));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (Group g : membergroups)
                    {
                        if (!groupIDSet.contains(g.getID()))
                        {
                            groupService.removeMember(c, group, g);
                        }
                    }

                }
                else
                {
                    // no members found (ids == null), remove them all!
                    for (Group g : membergroups)
                    {
                        groupService.removeMember(c, group, g);
                    }
                }

                groupService.update(c, group);

                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                c.complete();
            }
            else if (submit_group_delete)
            {
                // direct to a confirmation step
                request.setAttribute("group", group);
                JSPManager.showJSP(request, response, "/dspace-admin/group-confirm-delete.jsp");
            }
            else if (submit_confirm_delete)
            {
                // phony authorize, only admins can do this
                authorizeService.authorizeAction(c, group, Constants.WRITE);

                // delete group, return to group-list.jsp
                groupService.delete(c, group);

                showMainPage(c, request, response);
            }
            else if (submit_cancel_delete)
            {
                // show group list
                showMainPage(c, request, response);
            }
            else
            {
                // unknown action, show edit page
                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
            }
        }
        else
        // no group set
        {
            // want to add a group - create a blank one, and pass to
            // group_edit.jsp
            String button = UIUtil.getSubmitButton(request, "submit");

            if (button.equals("submit_add"))
            {
                group = groupService.create(c);

                group.setName(c, "new group" + group.getID());
                groupService.update(c, group);

                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                c.complete();
            }
            else
            {
                // show the main page (select groups)
                showMainPage(c, request, response);
            }
        }
    }

    private void showMainPage(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        List<Group> groups = groupService.findAll(c, GroupService.NAME);

        // if( groups == null ) { System.out.println("groups are null"); }
        // else System.out.println("# of groups: " + groups.length);
        request.setAttribute("groups", groups);

        JSPManager.showJSP(request, response, "/tools/group-list.jsp");
        c.complete();
    }
}
