<%--
  - bitstreams.jsp
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
  - Displays an item's disseminations.  At the moment, this is just a flat
  - list of bitstreams.
  -
  - FIXME: Just displays non-internal bitstreams as a flat list - should
  - display bundle structure or something
  -
  - Attributes:
  -    handle      - Handle of the item, if any
  -    item        - the Item to display
  --%>

<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Item" %>


<%
    // Weird variable names are to avoid clashes with pages that include this
    String handleCB = (String) request.getAttribute("handle");
    Item itemCB = (Item) request.getAttribute("item");
%>

<table align=center class="miscTable">
    <tr>
        <td class=evenRowEvenCol>
            <P><strong>Files:</strong></P>
<%
    Bundle[] bundlesCB = itemCB.getBundles();
    
    if (bundlesCB.length == 0)
    {
%>
            <P>There are no files associated with this item.</P>
<%
    }
    else
    {        
%>
            <UL>
<%
        for (int iCB = 0; iCB < bundlesCB.length; iCB++)
        {
            Bitstream[] bitstreamsCB = bundlesCB[iCB].getBitstreams();
            
            for (int kCB = 0; kCB < bitstreamsCB.length ; kCB++)
            {
                // Skip internal types
                if (!bitstreamsCB[kCB].getFormat().isInternal())
                {
                    String pathCB = "";
                    if (handleCB != null)
                    {
                        pathCB = handleCB + "/" + bundlesCB[iCB].getID() + "/";
                    }
%>
                <LI>
                    <A TARGET=_blank HREF="<%= request.getContextPath() %>/retrieve/<%= pathCB %><%= bitstreamsCB[kCB].getID() %>"><%= bitstreamsCB[kCB].getName() %></A>
                    (<%= bitstreamsCB[kCB].getSize() %> bytes; <%= bitstreamsCB[kCB].getFormatDescription() %>)
                </LI>

<%
                }
            }
        }
%>
            </UL>
<%
    }
%>
        </td>
    </tr>
</table>
