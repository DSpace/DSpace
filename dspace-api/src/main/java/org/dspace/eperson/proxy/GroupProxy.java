/*
 * GroupProxy.java
 *
 * Version: $Revision: 87 $
 *
 * Date: $Date: 2007-06-09 16:48:53 +0100 (Sat, 09 Jun 2007) $
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
package org.dspace.eperson.proxy;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Proxy class that sits in place of the Group class and refrains from loading
 * member EPersons and Groups into memory until explicitly requested.
 *
 * @author James Rutherford
 * @version $Revision: 87 $
 */
public class GroupProxy extends Group
{
    private boolean epeopleChanged = false;
    private boolean groupsChanged = false;

    private boolean epeopleLoaded = false;
    private boolean groupsLoaded = false;

    public GroupProxy(Context context, int id)
    {
        super(context, id);
    }

    @Override
    public boolean isEmpty()
    {
        if (!epeopleLoaded)
        {
            loadEPeople();
        }

        if (!groupsLoaded)
        {
            loadGroups();
        }

        return (epeople.size() == 0) && (groups.size() == 0);
    }

    @Override
    public void addMember(EPerson e)
    {
        if (!epeopleLoaded)
        {
            loadEPeople();
        }

        if (isMember(e))
        {
            return;
        }

        epeople.add(e);
        epeopleChanged = true;
    }

    @Override
    public void addMember(Group g)
    {
        if (!groupsLoaded)
        {
            loadGroups();
        }

        if (isMember(g))
        {
            return;
        }

        groups.add(g);
        groupsChanged = true;
    }

    @Override
    public void removeMember(EPerson e)
    {
        if (!epeopleLoaded)
        {
            loadEPeople();
        }

        if (epeople.remove(e))
        {
            epeopleChanged = true;
        }
    }

    @Override
    public void removeMember(Group g)
    {
        if (!groupsLoaded)
        {
            loadGroups();
        }

        if (groups.remove(g))
        {
            groupsChanged = true;
        }
    }

    @Override
    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (id == 0)
        {
            return true;
        }

        if (!epeopleLoaded)
        {
            loadEPeople();
        }

        return this.contains(epeople, e);
    }

    @Override
    public boolean isMember(Group g)
    {
        if (!groupsLoaded)
        {
            loadGroups();
        }

        return this.contains(groups, g);
    }

    @Override
    public Group[] getMemberGroups()
    {
        if (!groupsLoaded)
        {
            loadGroups();
        }

        return groups.toArray(new Group[0]);
    }

    @Override
    public EPerson[] getMembers()
    {
        if (!epeopleLoaded)
        {
            loadEPeople();
        }

        return epeople.toArray(new EPerson[0]);
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void loadEPeople()
    {
        if (!epeopleLoaded)
        {
            epeople = epersonDAO.getEPeople(this);
            epeopleLoaded = true;
        }
    }

    private void loadGroups()
    {
        if (!groupsLoaded)
        {
            groups = dao.getMemberGroups(this);
            groupsLoaded = true;
        }
    }
}
