<%--
  - authorize_collection_edit.jsp
  -
  - $Id$
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>


<%--
  - Show policies for a collection, allowing you to modify, delete
  -  or add to them
  -
  - Attributes:
  -  collection - Collection being modified
  -  policies - ResourcePolicy [] of policies for the collection
  - Returns:
  -  submit value collection_addpolicy    to add a policy
  -  submit value collection_editpolicy   to edit policy
  -  submit value collection_deletepolicy to delete policy
  -
  -  policy_id - ID of policy to edit, delete
  -
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.List"     %>
<%@ page import="java.util.Iterator" %>


<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    List policies =
        (List) request.getAttribute("policies");
%>

<dspace:layout title="Edit collection policies"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Policies for collection <%= collection.getMetadata("name") %></h1>

    <P align="center">
        <form method=POST>
            <input type="hidden" name="collection_id" value="<%=collection.getID()%>" >
            <input type="submit" name="submit_collection_add_policy" value="Add New">
        </form>
    </p>


    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong>ID</strong></th>
            <th class="oddRowEvenCol"><strong>Action</strong></th>
            <th class="oddRowOddCol"><strong>Public</strong></th>
            <th class="oddRowEvenCol"><strong>EPerson</strong></th>
            <th class="oddRowOddCol"><strong>Group</strong></th>
            <th class="oddRowEvenCol"><strong>StartDate</strong></th>
            <th class="oddRowOddCol"><strong>EndDate</strong></th>
            <th class="oddRowEvenCol">&nbsp;</th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    Iterator i = policies.iterator();

    while( i.hasNext() )
    {
        ResourcePolicy rp = (ResourcePolicy) i.next();
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= Constants.actiontext[rp.getAction()]%>
                </td>
                <td class="<%= row %>RowOddCol">
                    ...  
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= (rp.getEPerson() == null ? "..." : rp.getEPerson().getEmail() ) %>  
                </td>
                <td class="<%= row %>RowOddCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>  
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= (rp.getStartDate() == null ? "..." : "..." ) %>  
                </td>
                <td class="<%= row %>RowOddCol">
                    <%= (rp.getEndDate() == null   ? "..." : "..." ) %>  
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="hidden" name="policy_id"     value="<%= rp.getID() %>">
                    <input type="hidden" name="collection_id" value="<%= collection.getID() %>">
                    <input type="submit" name="submit_collection_edit_policy" value="Edit">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="submit" name="submit_collection_delete_policy" value="Delete">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
        
</dspace:layout>
