<%--
  - edit-item-form.jsp
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
  - Show form allowing edit of collection metadata
  -
  - Attributes:
  -    item        - item to edit
  -    collections - collections the item is in, if any
  -    handle      - item's Handle, if any (String)
  -    dc.types    - DCType[] - all DC types in the registry
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%@ page import="org.dspace.administer.DCType" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    Item item = (Item) request.getAttribute("item");
    String handle = (String) request.getAttribute("handle");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    DCType[] dcTypes = (DCType[])  request.getAttribute("dc.types");
%>

<dspace:layout title="Edit Item"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">
    <H1>Edit Item</H1>
    
    <P><strong>PLEASE NOTE: These changes are not validated in any way.
    You are responsible for entering the data in the correct format.
    If you are not sure what the format is, please do NOT make changes.</strong></P>  

    <center>
        <table width="70%">
            <tr>
                <td class="submitFormLabel">Item&nbsp;internal&nbsp;ID:</td>
                <td class="standard"><%= item.getID() %></td>
                <td class="standard" width="100%" align="right" rowspan=5>
<%
    if (!item.isWithdrawn())
    {
%>
                    <form method=POST action="<%= request.getContextPath() %>/dspace-admin/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>">
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_WITHDRAW %>">
                        <input type="submit" name="submit" value="Withdraw...">
                    </form>
<%
    }
    else
    {
%>
                    <form method=POST action="<%= request.getContextPath() %>/dspace-admin/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>">
                        <input type="hidden" name="action" value="<%= EditItemServlet.REINSTATE %>">
                        <input type="submit" name="submit" value="Reinstate">
                    </form>
<%
    }
%>
                    <br>
                    <form method=POST action="<%= request.getContextPath() %>/dspace-admin/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>">
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_DELETE %>">
                        <input type="submit" name="submit" value="Delete (Expunge)...">
                    </form>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel">Handle:</td>
                <td class="standard"><%= (handle == null ? "None" : handle) %></td>
            </tr>
                <td class="submitFormLabel">Last modified:</td>
                <td class="standard"><dspace:date date="<%= new DCDate(item.getLastModified()) %>" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel">In Collections:</td>
                <td class="standard">
<%  for (int i = 0; i < collections.length; i++) { %>
                    <%= collections[i].getMetadata("name") %><br>
<%  } %>
                </td>
            </tr>

            <tr>
                <td class="submitFormLabel">Item page:</td>
                <td class="standard">
<%  if (handle == null) { %>
                    <em>N/A</em>
<%  } else {
    String url = ConfigurationManager.getProperty("dspace.url") + "/handle/" + handle; %>
                    <A TARGET=_blank HREF="<%= url %>"><%= url %></A>
<%  } %>
                </td>
            </tr>
        </table>
    </center>

<%


    if (item.isWithdrawn())
    {
%>
    <P align=center><strong>This item was withdrawn from DSpace</strong></P>
<%
    }
%>
    <P>&nbsp;</P>


    <form method=POST action="<%= request.getContextPath() %>/dspace-admin/edit-item">
        <table class="miscTable">
            <tr>
                <th class="oddRowOddCol"><strong>Element</strong></th>
                <th class="oddRowEvenCol"><strong>Qualifier</strong></th>
                <th class="oddRowOddCol"><strong>Value</strong></th>
                <th class="oddRowEvenCol"><strong>Language</strong></th>
                <th class="oddRowOddCol">&nbsp;</th>
            </tr>
<%
    DCValue[] dcv = item.getDC(Item.ANY, Item.ANY, Item.ANY);
    String row = "even";
    
    // Keep a count of the number of values of each element+qualifier
    // key is "element" or "element_qualifier" (String)
    // values are Integers - number of values that element/qualifier so far
    Map dcCounter = new HashMap();
    
    for (int i = 0; i < dcv.length; i++)
    {
        // Find out how many values with this element/qualifier we've found

        String key = dcv[i].element +
            (dcv[i].qualifier == null ? "" : "_" + dcv[i].qualifier);

        Integer count = (Integer) dcCounter.get(key);
        if (count == null)
        {
            count = new Integer(0);
        }
        
        // Increment counter in map
        dcCounter.put(key, new Integer(count.intValue() + 1));

        // We will use two digits to represent the counter number in the parameter names.
        // This means a string sort can be used to put things in the correct order even
        // if there are >= 10 values for a particular element/qualifier.  Increase this to 
        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
        String sequenceNumber = count.toString();
        
        while (sequenceNumber.length() < 2)
        {
            sequenceNumber = "0" + sequenceNumber;
        }
 %>
            <tr>
                <td class="<%= row %>RowOddCol"><%= dcv[i].element %>&nbsp;&nbsp;</td>
                <td class="<%= row %>RowEvenCol"><%= (dcv[i].qualifier == null ? "" : dcv[i].qualifier) %></td>
                <td class="<%= row %>RowOddCol">
                    <textarea name="value_<%= key %>_<%= sequenceNumber %>" rows=3 cols=50><%= dcv[i].value %></textarea>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv[i].language == null ? "" : dcv[i].language) %>" size=5>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_remove_<%= key %>_<%= sequenceNumber %>" value="Remove">
                </td>
            </tr>
<%      row = (row.equals("odd") ? "even" : "odd");
    } %>

            <tr><td>&nbsp;</td></tr>

            <tr>
                <td colspan=2 class="<%= row %>RowEvenCol">
                    <select name="addfield_dctype">
<%  for (int i = 0; i < dcTypes.length; i++) { %>
                        <option value="<%= dcTypes[i].getID() %>"><%= dcTypes[i].getElement() %><%= (dcTypes[i].getQualifier() == null ? "" : "." + dcTypes[i].getQualifier()) %></option>
<%  } %>
                    </select>
                </td>
                <td class="<%= row %>RowOddCol">
                    <textarea name="addfield_value" rows=3 cols=50></textarea>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="addfield_language" size=5>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_addfield" value="Add">
                </td>
            </tr>
        </table>
        
        <P>&nbsp;</P>

        <H2>Bitstreams</H2>

        <P><strong>Note: Changes to the bitstreams will not be automatically reflected in the
        Dublin Core metadata above (e.g. <code>format.extent</code>, <code>format.mimetype</code>).
        You will need to update this by hand.</strong></P>

        <P>Also note that if the "user format description" field isn't empty, the format will
        always be set to "Unknown", so clear the user format description before changing the
        format field.</P>

        <table class="miscTable">
            <tr>
                <th class="oddRowOddCol"><strong>Name</strong></th>
                <th class="oddRowEvenCol"><strong>Source</strong></th>
                <th class="oddRowOddCol"><strong>Description</strong></th>
                <th class="oddRowEvenCol"><strong>Format</strong></th>
                <th class="oddRowOddCol"><strong>User&nbsp;Format&nbsp;Description</strong></th>
                <th class="oddRowEvenCol">&nbsp;</th>
            </tr>
<%
    Bundle[] bundles = item.getBundles();
    row = "even";

    for (int i = 0; i < bundles.length; i++)
    {
        Bitstream[] bitstreams = bundles[i].getBitstreams();
        for (int j = 0; j < bitstreams.length; j++)
        {
            // Parameter names will include the bundle and bitstream ID
            // e.g. "bitstream_14_18_desc" is the description of bitstream 18 in bundle 14
            String key = bundles[i].getID() + "_" + bitstreams[j].getID();
            BitstreamFormat bf = bitstreams[j].getFormat();
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_name_<%= key %>" value="<%= (bitstreams[j].getName() == null ? "" : bitstreams[j].getName()) %>">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="bitstream_source_<%= key %>" value="<%= (bitstreams[j].getSource() == null ? "" : bitstreams[j].getSource()) %>">
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_description_<%= key %>" value="<%= (bitstreams[j].getDescription() == null ? "" : bitstreams[j].getDescription()) %>">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="bitstream_format_id_<%= key %>" value="<%= bf.getID() %>" size="4"> (<%= bf.getShortDescription() %>)
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_user_format_description_<%= key %>" value="<%= (bitstreams[j].getUserFormatDescription() == null ? "" : bitstreams[j].getUserFormatDescription()) %>">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <A TARGET=_blank HREF="<%= request.getContextPath() %>/retrieve/<%= bitstreams[j].getID() %>">View</A>&nbsp;<input type="submit" name="submit_delete_bitstream_<%= key %>" value="Remove">
                </td>
            </tr>
<%
            row = (row.equals("odd") ? "even" : "odd");
        }
    }
%>
        </table>

        <P align="center"><input type="submit" name="submit_addbitstream" value="Add Bitstream"></P>

        <P>&nbsp;</P>

        <input type="hidden" name="item_id" value="<%= item.getID() %>">
        <input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_ITEM %>">
        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <input type="submit" name="submit" value="Update">
                    </td>
                    <td align="right">
                        <input type="submit" name="submit_cancel" value="Cancel">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
