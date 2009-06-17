/*
 * GroupEditServlet.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Servlet for editing groups
 * 
 * @author dstuve
 * @version $Revision: 3705 $
 */
public class GroupEditServlet extends DSpaceServlet
{
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
        int groupID = UIUtil.getIntParameter(request, "group_id");
        Group group = null;

        if (groupID >= 0)
        {
            group = Group.find(c, groupID);
        }

        // group is set
        if (group != null)
        {
            // is this user authorized to edit this group?
            AuthorizeManager.authorizeAction(c, group, Constants.ADD);

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
                    group.setName(newName);
                    group.update();
                }

                int[] eperson_ids = UIUtil.getIntParameters(request,
                        "eperson_id");
                int[] group_ids = UIUtil.getIntParameters(request, "group_ids");

                // now get members, and add new ones and remove missing ones
                EPerson[] members = group.getMembers();
                Group[] membergroups = group.getMemberGroups();

                if (eperson_ids != null)
                {
                    // some epeople were listed, now make group's epeople match
                    // given epeople
                    Set memberSet = new HashSet();
                    Set epersonIDSet = new HashSet();

                    // add all members to a set
                    for (int x = 0; x < members.length; x++)
                    {
                        Integer epersonID = new Integer(members[x].getID());
                        memberSet.add(epersonID);
                    }

                    // now all eperson_ids are put in a set
                    for (int x = 0; x < eperson_ids.length; x++)
                    {
                        epersonIDSet.add(new Integer(eperson_ids[x]));
                    }

                    // process eperson_ids, adding those to group not already
                    // members
                    Iterator i = epersonIDSet.iterator();

                    while (i.hasNext())
                    {
                        Integer currentID = (Integer) i.next();

                        if (!memberSet.contains(currentID))
                        {
                            group.addMember(EPerson.find(c, currentID
                                    .intValue()));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (int x = 0; x < members.length; x++)
                    {
                        EPerson e = members[x];

                        if (!epersonIDSet.contains(new Integer(e.getID())))
                        {
                            group.removeMember(e);
                        }
                    }
                }
                else
                {
                    // no members found (ids == null), remove them all!

                    for (int y = 0; y < members.length; y++)
                    {
                        group.removeMember(members[y]);
                    }
                }

                if (group_ids != null)
                {
                    // some groups were listed, now make group's member groups
                    // match given group IDs
                    Set memberSet = new HashSet();
                    Set groupIDSet = new HashSet();

                    // add all members to a set
                    for (int x = 0; x < membergroups.length; x++)
                    {
                        Integer myID = new Integer(membergroups[x].getID());
                        memberSet.add(myID);
                    }

                    // now all eperson_ids are put in a set
                    for (int x = 0; x < group_ids.length; x++)
                    {
                        groupIDSet.add(new Integer(group_ids[x]));
                    }

                    // process group_ids, adding those to group not already
                    // members
                    Iterator i = groupIDSet.iterator();

                    while (i.hasNext())
                    {
                        Integer currentID = (Integer) i.next();

                        if (!memberSet.contains(currentID))
                        {
                            group
                                    .addMember(Group.find(c, currentID
                                            .intValue()));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (int x = 0; x < membergroups.length; x++)
                    {
                        Group g = membergroups[x];

                        if (!groupIDSet.contains(new Integer(g.getID())))
                        {
                            group.removeMember(g);
                        }
                    }

                }
                else
                {
                    // no members found (ids == null), remove them all!
                    for (int y = 0; y < membergroups.length; y++)
                    {
                        group.removeMember(membergroups[y]);
                    }
                }

                group.update();

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
                AuthorizeManager.authorizeAction(c, group, Constants.WRITE);

                // delete group, return to group-list.jsp
                group.delete();

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
                group = Group.create(c);

                group.setName("new group" + group.getID());
                group.update();

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
        Group[] groups = Group.findAll(c, Group.NAME);

        // if( groups == null ) { System.out.println("groups are null"); }
        // else System.out.println("# of groups: " + groups.length);
        request.setAttribute("groups", groups);

        JSPManager.showJSP(request, response, "/tools/group-list.jsp");
        c.complete();
    }
}
