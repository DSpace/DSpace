<%--
  - select-collection.jsp
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
  - UI page for selection of collection.
  -
  - Required attributes:
  -    collections - Array of collection objects to show in the drop-down.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.select-collection.title"
               nocache="true">

    <jsp:include page="/submit/progressbar.jsp">
        <jsp:param name="current_stage" value="<%= SubmitServlet.SELECT_COLLECTION %>"/>
        <jsp:param name="stage_reached" value="0"/>
        <jsp:param name="md_pages" value="1"/>
    </jsp:include>

    <h1><fmt:message key="jsp.submit.select-collection.heading"/></h1>

	
<%  if (collections.length > 0)
    {
%>
	<div><fmt:message key="jsp.submit.select-collection.info1"/>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#choosecollection\"%>"><fmt:message key="jsp.morehelp"/> </dspace:popup> 
	</div>

    <form action="<%= request.getContextPath() %>/submit" method="post">
<%-- HACK: a <center> tag seems to be the only way to convince certain --%>
<%--       browsers to center the table. --%>
        <center>
            <table summary="Select collection table">
                <tr>
                    <%-- <td class="submitFormLabel"><label for="tcollection">Collection</label></td> --%>
					<td class="submitFormLabel"><label for="tcollection"><fmt:message key="jsp.submit.select-collection.collection"/></label></td>
                    <td>
                        <select name="collection" id="tcollection">
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
                            <option value="<%= collections[i].getID() %>"><%= collections[i].getMetadata("name") %></option>
<%
        }
%>
                        </select>
                    </td>
                </tr>
            </table>

            <%-- Hidden field indicating the step --%>
            <input type="hidden" name="step" value="<%= SubmitServlet.SELECT_COLLECTION %>" />
            <br />

            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <%-- <input type="submit" name="submit_next" value="Next &gt;"> --%>
						<input type="submit" name="submit_next" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel/Save"> --%>
						<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
<%  } else { %>
	<p class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>	
	   <p><fmt:message key="jsp.general.goto"/><br />
	   <a href="<%= request.getContextPath() %>"><fmt:message key="jsp.general.home"/></a><br />
	   <a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.general.mydspace" /></a>
	   </p>	
</dspace:layout>
