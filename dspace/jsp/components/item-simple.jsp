<%--
  - item-simple.jsp
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
  - Displays the default set of item metadata, with links to full content -
  - This is not a full page, just a component to be included
  -
  - Attributes:
  -    handle      - Handle of the item, if any
  -    item        - the Item to display
  -    collections - Array of Collections this item appears in.  This must be
  -                  passed in for two reasons: 1) item.getCollections() could
  -                  fail, and we're already committed to JSP display, and
  -                  2) the item might be in the process of being submitted and
  -                  a mapping between the item and collection might not
  -                  appear yet.  If this is omitted, the item display won't
  -                  display any collections.
  -    communities - Array of communities corresponding to above collections.
  -                  i.e. communities[n] contains collections[n].  Must be
  -                  supplied if collections is supplied.
  --%>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>

<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%
    Item item = (Item) request.getAttribute("item");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    Community[] communities = (Community[]) request.getAttribute("communities");

    // Build up a list of things to display.
    // To display a DC field from the item, do
    //   fields.add(new String[] {"Display Name", "element", "qualifier"});
    //      (or "qualifier" as null for unqualified)
    // to display an actual value without getting it from the item, do
    //   fields.add(new String[] {"Display Name", "The value to display")


    List fields = new LinkedList();
    
    // Title - special case, if there is no title, use "Untitled"
    DCValue[] titleDC = item.getDC("title", null, Item.ANY);
    if (titleDC.length == 0)
    {
        fields.add(new String[] {"Title", "Untitled"});
    }
    else
    {
        fields.add(new String[] {"Title", "title", null});
    }

    fields.add(new String[] {"Other Titles", "title", "alternative"});
    fields.add(new String[] {"Authors", "contributor", "author"});
    fields.add(new String[] {"Keywords", "subject", null});
    // FIXME: Should display date properly
    fields.add(new String[] {"Issue Date", "date", "issued"});
    fields.add(new String[] {"Series/Report no.", "relation", "ispartofseries"});
    fields.add(new String[] {"Abstract", "description", "abstract"});
    fields.add(new String[] {"Description", "description", null});
    fields.add(new String[] {"Gov't Doc # ", "identifier", "govdoc"});
    fields.add(new String[] {"URI", "identifier", "uri"});
    fields.add(new String[] {"ISBN", "identifier", "isbn"});
    fields.add(new String[] {"ISSN", "identifier", "issn"});
    fields.add(new String[] {"ISMN", "identifier", "ismn"});
    fields.add(new String[] {"Other Identifiers", "identifier", null});
%>

<center>

<table class="itemDisplayTable">

<%
    Iterator fieldIterator = fields.iterator();
    while (fieldIterator.hasNext())
    {
        String[] fieldData = (String[]) fieldIterator.next();
        DCValue[] values;

        if (fieldData.length == 2)
        {
            // Value direct from field data
            DCValue v = new DCValue();
            v.value = fieldData[1];
            values = new DCValue[1];
            values[0] = v;
        }
        else
        {
            // Grab the value from the item
            values = item.getDC(fieldData[1], fieldData[2], Item.ANY);
        }

        // Only display the field if we have an actual value
        if (values.length > 0)
        {
%>
    <tr>
        <td class="metadataFieldLabel"><%= fieldData[0] %>:</td>
        <td class="metadataFieldValue"><%= values[0].value %>
<%
            for (int j = 1; j < values.length; j++)
            {
%>
    <br><%= values[j].value %>
<%
            }
%>
        </td>
    </tr>
<%
        }
    }    
%>


    <%-- Collections --%>
    <tr>
        <td class="metadataFieldLabel">Appears in Collections:</td>
        <td class="metadataFieldValue">
<%
    for (int i = 0; i < collections.length; i++)
    {
%>
            <A HREF="<%= request.getContextPath() %>/communities/<%= communities[i].getID() %>/collections/<%= collections[i].getID() %>/"><%= collections[i].getMetadata("name") %></A><BR>
<%
    }
%>
        </td>
    </tr>
</table>

</center>

<br>

<%@ include file="/components/bitstreams.jsp" %>
