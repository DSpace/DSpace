<%--
  - select-collection.jsp
  -
  - Version: $Revision: 1.10 $
  -
  - Date: $Date: 2005/02/07 21:56:36 $
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

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
%>

<dspace:layout locbar="off" navbar="off" title="Select Collection to Submit to" nocache="true">

    <jsp:include page="/submit/progressbar.jsp">
        <jsp:param name="current_stage" value="<%= SubmitServlet.SELECT_COLLECTION %>"/>
        <jsp:param name="stage_reached" value="0"/>
        <jsp:param name="md_pages" value="1"/>
    </jsp:include>

    <H1>Submit: Choose Collection</H1>

    <p>Select the collection you wish to submit an item to from the list
    below, then click "Next".  
    <dspace:popup page="/help/index.html#choosecollection">(More Help...)</dspace:popup></p>

    <form action="<%= request.getContextPath() %>/submit" method=post>
<%-- HACK: a <center> tag seems to be the only way to convince certain --%>
<%--       browsers to center the table. --%>
        <center>
            <table>
                <tr>
                    <td class="submitFormLabel">Collection</td>
                    <td>
                        <select name=collection>
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
            <input type="hidden" name="step" value="<%= SubmitServlet.SELECT_COLLECTION %>">
            <br>

            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type=submit name=submit_next value="Next &gt;">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align=right>
                        <input type=submit name=submit_cancel value="Cancel/Save">
                    </td>
                </tr>
            </table>


        </center>
    </form>
    
</dspace:layout>
