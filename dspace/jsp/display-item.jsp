<%--
  - display-item.jsp
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
  - Renders a whole HTML page for displaying item metadata.  Simply includes
  - the relevant item display component in a standard HTML page.
  -
  - Attributes:
  -    display.all - Boolean - if true, display full metadata record
  -    item        - the Item to display
  -    collections - Array of Collections this item appears in.  This must be
  -                  passed in for two reasons: 1) item.getCollections() could
  -                  fail, and we're already committed to JSP display, and
  -                  2) the item might be in the process of being submitted and
  -                  a mapping between the item and collection might not
  -                  appear yet.  If this is omitted, the item display won't
  -                  display any collections.
  -    admin_button - Boolean, show admin 'edit' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.handle.HandleManager" %>
<%@ page import="org.dspace.license.CC" %>

<%
    // Attributes
    Boolean displayAllBoolean = (Boolean) request.getAttribute("display.all");
    boolean displayAll = (displayAllBoolean != null && displayAllBoolean.booleanValue());
    Item item = (Item) request.getAttribute("item");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
    String handle = item.getHandle();

    // CC URL & RDF
    String cc_url = CC.getLicenseURL(item);
    String cc_rdf = CC.getLicenseRDF(item);

    // Full title needs to be put into a string to use as tag argument
    String title = "Item " + handle;
%>

<dspace:layout title="<%= title %>">

<%
    if (handle != null)
    {
%>
    <table align=center class=miscTable>
        <tr>
            <td class=evenRowEvenCol align="center">
                <strong>Please use this identifier to cite or link to this item:
                <code><%= HandleManager.getCanonicalForm(handle) %></code></strong>
            </td>
<%
        if (admin_button)  // admin edit button
        { %>
			<td class=evenRowEvenCol align="center">
				<form method=GET action="<%= request.getContextPath() %>/tools/edit-item">
					<input type="hidden" name="item_id" value="<%= item.getID() %>">
				    <input type="submit" name="submit" value="Edit...">
				</form>
			</td>
<%      } %>
        </tr>
    </table>
    <br>
<%
    }

    String displayStyle = (displayAll ? "full" : "default");
%>
    <dspace:item item="<%= item %>" collections="<%= collections %>" style="<%= displayStyle %>" />

<%
    String locationLink = request.getContextPath() + "/handle/" + handle;
    
    if (displayAll)
    {
%>
    <P align=center><A HREF="<%= locationLink %>?mode=simple">Show simple item record</A></P>
<%
    }
    else
    {
%>
    <P align=center><A HREF="<%= locationLink %>?mode=full">Show full item record</A></P>
<%
    }
%>

    <%-- SFX Link --%>
<%
    if (ConfigurationManager.getProperty("sfx.server.url") != null)
    {
%>
    <P align=center>
        <A HREF="<dspace:sfxlink item="<%= item %>" />"><IMG SRC="<%= request.getContextPath() %>/image/sfx-link.gif" BORDER=0 ALT="SFX Query"></A>
    </P>
<%
    }
%>
    <%-- Create Commons Link --%>
<%
    if (cc_url != null)
    {
%>
    <P class="submitFormHelp">This item is licensed inder a <a href="<%= cc_url %>">Creative Commons License</a><br/>
    <a href="<%= cc_url %>"><img src="<%= request.getContextPath() %>/image/cc-somerights.gif" border="0" ALT="Creative Commons" /></a>
    </P>
    <!--
    <%= cc_rdf %>
    -->
<%
    }
%>
    <P class="submitFormHelp">All items in DSpace are protected by copyright, with all rights reserved.</P>

</dspace:layout>
