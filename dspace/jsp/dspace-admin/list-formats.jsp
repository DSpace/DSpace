<%--
  - list-formats.jsp
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
  - Display list of bitstream formats
  -
  - Attributes:
  -
  -   formats - the bitstream formats in the system (BitstreamFormat[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>



<%
    BitstreamFormat[] formats =
        (BitstreamFormat[]) request.getAttribute("formats");
%>

<dspace:layout titlekey="jsp.dspace-admin.list-formats.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.list-formats.title"/></h1>

    <p><fmt:message key="jsp.dspace-admin.list-formats.text1"/></p>
    <p><fmt:message key="jsp.dspace-admin.list-formats.text2"/></p>

    &nbsp;&nbsp;<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#bitstream\"%>"><fmt:message key="jsp.help"/></dspace:popup>

<%
    Context context = UIUtil.obtainContext(request);

%>

        <table class="miscTable" align="center" summary="Bitstream Format Registry data table">
            <tr>
                <th class="oddRowOddCol">
                    <strong>
                            <fmt:message key="jsp.general.id" />
                            / <fmt:message key="jsp.dspace-admin.list-formats.mime"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.name"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.description"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.support"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.internal"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.extensions"/>
                    </strong>
                 </th>
            </tr>
<%

    String row = "even";
    for (int i = 0; i < formats.length; i++)
    {
        String[] extensions = formats[i].getExtensions();
        String extValue = "";

        for (int j = 0 ; j < extensions.length; j++)
        {
            if (j > 0)
            {
                extValue = extValue + ", ";
            }
            extValue = extValue + extensions[j];
        }
%>
             <tr>
                 <td>
                  <form method="post" action="">
                    <table>
                       <tr>
                          <td class="<%= row %>RowOddCol"><%= formats[i].getID() %></td>
                          <td class="<%= row %>RowEvenCol">
                              <input type="text" name="mimetype" value="<%= formats[i].getMIMEType() %>" size="14"/>
                          </td>
                          <td class="<%= row %>RowOddCol">
                    <%
                      if (BitstreamFormat.findUnknown(context).getID() == formats[i].getID()) {
                    %>
                      <i><%= formats[i].getShortDescription() %></i>
                    <% } else { %>
                              <input type="text" name="short_description" value="<%= formats[i].getShortDescription() %>" size="10"/>
                    <% } %>
                          </td>
                          <td class="<%= row %>RowEvenCol">
                              <input type="text" name="description" value="<%= formats[i].getDescription() %>" size="20"/>
                          </td>
                          <td class="<%= row %>RowOddCol">
                              <select name="support_level">
                                  <option value="0" <%= formats[i].getSupportLevel() == 0 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.unknown"/></option>
	    	                  <option value="1" <%= formats[i].getSupportLevel() == 1 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.known"/></option>
                                  <option value="2" <%= formats[i].getSupportLevel() == 2 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.supported"/></option>
                              </select>
                          </td>
                          <td class="<%= row %>RowEvenCol" align="center">
                              <input type="checkbox" name="internal" value="true"<%= formats[i].isInternal() ? " checked=\"checked\"" : "" %>/>
                          </td>
                          <td class="<%= row %>RowOddCol">
                              <input type="text" name="extensions" value="<%= extValue %>" size="10"/>
                          </td>
                          <td class="<%= row %>RowEvenCol">
                              <input type="hidden" name="format_id" value="<%= formats[i].getID() %>" />
                              <input type="submit" name="submit_update" value="<fmt:message key="jsp.dspace-admin.general.update"/>"/>
                          </td>
                          <td class="<%= row %>RowOddCol">
                    <%
                      if (BitstreamFormat.findUnknown(context).getID() != formats[i].getID()) {
                    %>
                             <input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" />
                     <% 
                      } 
                    %>
                         </td>
                    </tr>    
                  </table> 
                 </form>
             </td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>

  </table>

  <form method="post" action="">
    <p align="center">
            <input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.general.addnew"/>" />
    </p>
  </form>
</dspace:layout>
