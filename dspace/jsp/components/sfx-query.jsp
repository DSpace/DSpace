<%--
  - sfx-query.jsp
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
  - Component for showing an outgoing SFX link for an item
  -
  - Attributes:
  -    item        - the Item to display the SFX query for
  --%>

<%@ page import="java.net.URLEncoder" %>

<%@ page import="org.dspace.content.DCPersonName" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    // Check SFX is supported - it is if we have an SFX server
    String sfxServer = ConfigurationManager.getProperty("sfx.server.url");

    if (sfxServer != null)
    {
        Item item = (Item) request.getAttribute("item");

        String sfxQuery = "";

        DCValue[] titles = item.getDC("title", null, Item.ANY);
        if (titles.length > 0)
        {
            sfxQuery = sfxQuery + "&title=" + URLEncoder.encode(titles[0].value);
        }

        DCValue[] authors = item.getDC("contributor", "author", Item.ANY);
        if (authors.length > 0)
        {
            DCPersonName dpn = new DCPersonName(authors[0].value);
            sfxQuery = sfxQuery + "&aulast=" + URLEncoder.encode(dpn.getLastName());
            sfxQuery = sfxQuery + "&aufirst=" + URLEncoder.encode(dpn.getFirstNames());
        }

        DCValue[] isbn = item.getDC("identifier", "isbn", Item.ANY);
        if (isbn.length > 0)
        {
            sfxQuery = sfxQuery + "&isbn=" + URLEncoder.encode(isbn[0].value);
        }

        DCValue[] issn = item.getDC("identifier", "issn", Item.ANY);
        if (issn.length > 0)
        {
            sfxQuery = sfxQuery + "&issn=" + URLEncoder.encode(issn[0].value);
        }

        DCValue[] dates = item.getDC("date", "issued", Item.ANY);
        if (dates.length > 0)
        {
            String fullDate = dates[0].value;
            // Remove the time if there is one - day is greatest granularity for SFX
            if (fullDate.length() > 10)
            {
                fullDate = fullDate.substring(0, 10);
            }
            sfxQuery = sfxQuery + "&date=" + URLEncoder.encode(fullDate);
        }

        // Remove initial &, if any
        if (sfxQuery.startsWith("&"))
        {
            sfxQuery = sfxQuery.substring( 1 );
        }
%>
    <P align=center><A HREF="<%= sfxServer %><%= sfxQuery %>">SFX Query</A></P>
<%
    }
%>
