<%--
  - authorize_item_edit.jsp
  -
  - $Id$
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
  - Show policies for an item, allowing you to modify, delete
  -  or add to them
  -
  - Attributes:
  -  item          - Item being modified
  -  item_policies - ResourcePolicy List of policies for the item
  -  bundles            - [] of Bundle objects
  -  bundle_policies    - Map of (ID, List of policies)
  -  bitstream_policies - Map of (ID, List of policies)
  -
  - Returns:
  -  submit value item_add_policy         to add a policy 
  -  submit value item_edit_policy        to edit policy for item, bundle, or bitstream
  -  submit value item_delete_policy      to delete policy for item, bundle, or bitstream
  -
  -  submit value bundle_add_policy       add policy
  -
  -  submit value bitstream_add_policy    to add a policy
  -
  -  policy_id - ID of policy to edit, delete
  -  item_id
  -  bitstream_id
  -  bundle_id
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List"     %>
<%@ page import="java.util.Map"      %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Item"             %>
<%@ page import="org.dspace.content.Bundle"           %>
<%@ page import="org.dspace.content.Bitstream"        %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>

<%
    // get item and list of policies
    Item item = (Item) request.getAttribute("item");
    List item_policies =
        (List) request.getAttribute("item_policies");

    // get bitstreams and corresponding policy lists
    Bundle [] bundles      = (Bundle [])request.getAttribute("bundles");
    Map bundle_policies    = (Map)request.getAttribute("bundle_policies"   );
    Map bitstream_policies = (Map)request.getAttribute("bitstream_policies");
%>

<dspace:layout title="Edit item policies"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer"
               nocache="true">

    <h1>Policies for Item <%= item.getHandle() %> (ID=<%= item.getID() %>)</h1>

    <P>With this editor you can view and alter the policies of an item,
    plus alter policies of individual item components:  bundles and bitstreams.
    Briefly, an item is a container of bundles, and bundles, are containers
    of bitstreams.  Containers usually have ADD/REMOVE/READ/WRITE policies,
    while bitstreams only have READ/WRITE policies.
    </P>
    <P>You will notice an extra bundle and bitstream for each item, and those
    contain the license text for the item.
    </P>


    <H3>Item Policies</H3>
    <P align="center">
        <form method=POST>
            <input type="hidden" name="item_id" value="<%=item.getID()%>" >
            <input type="submit" name="submit_item_add_policy" value="Add New Policy">
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
    Iterator i = item_policies.iterator();

    while( i.hasNext() )
    {
        ResourcePolicy rp = (ResourcePolicy) i.next();
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
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
                    <input type="hidden" name="item_id"       value="<%= item.getID() %>">
                    <input type="submit" name="submit_item_edit_policy" value="Edit">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="submit" name="submit_item_delete_policy" value="Delete">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
<%
    for( int b = 0; b < bundles.length; b++ )
    {
        Bundle myBun = bundles[b];
        List myPolicies = (List)bundle_policies.get(new Integer(myBun.getID()));

        // display add policy
        // display bundle header w/ID

%>                
        <H3>Bundle <%=myBun.getID()%> Policies</H3>

        <P align="center">
            <form method=POST>
                <input type="hidden" name="item_id"   value="<%=item.getID()%>" >
                <input type="hidden" name="bundle_id" value="<%=myBun.getID()%>" >
                <input type="submit" name="submit_bundle_add_policy" value="Add New Policy">
            </form>
        </P>
        

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
    row = "even";
    i = myPolicies.iterator();

    while( i.hasNext() )
    {
        ResourcePolicy rp = (ResourcePolicy) i.next();
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
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
                    <input type="hidden" name="item_id"       value="<%= item.getID() %>">
                    <input type="hidden" name="bundle_id"     value="<%= myBun.getID() %>">
                    <input type="submit" name="submit_item_edit_policy" value="Edit">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="submit" name="submit_item_delete_policy" value="Delete">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<%
        Bitstream [] bitstreams = myBun.getBitstreams();
                
        for( int s = 0; s < bitstreams.length; s++ )
        {
            Bitstream myBits = bitstreams[s];
            myPolicies  = (List)bitstream_policies.get(new Integer(myBits.getID()));

            // display bitstream header w/ID, filename
            // 'add policy'
            // display bitstream's policies
%>                        
            <P>Bitstream <%=myBits.getID()%> (<%=myBits.getName()%>)</P>
            <P align="center">
                <FORM method=POST>
                    <input type="hidden" name="item_id"      value="<%=item.getID()%>">
                    <input type="hidden" name="bitstream_id" value="<%=myBits.getID()%>" >
                    <input type="submit" name="submit_bitstream_add_policy" value="Add New Policy">
                </FORM>
            </P>
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
    row = "even";
    i = myPolicies.iterator();

    while( i.hasNext() )
    {
        ResourcePolicy rp = (ResourcePolicy) i.next();
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
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
                    <input type="hidden" name="policy_id"     value="<%= rp.getID()     %>">
                    <input type="hidden" name="item_id"       value="<%= item.getID()   %>">
                    <input type="hidden" name="bitstream_id"  value="<%= myBits.getID() %>">
                    <input type="submit" name="submit_item_edit_policy" value="Edit">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="submit" name="submit_item_delete_policy" value="Delete">
                </td>
            </tr>
        </form>
<%
                row = (row.equals("odd") ? "even" : "odd");
            }
%>
    </table>
<%

        }
    }
%>
        
</dspace:layout>
