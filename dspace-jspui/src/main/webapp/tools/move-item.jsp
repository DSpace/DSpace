<%--
  - move-item.jsp
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

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
	
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	Collection [] notLinkedCollections = (Collection[] )request.getAttribute("notLinkedCollections");
	Collection [] linkedCollections = (Collection[] )request.getAttribute("linkedCollections");
	
	Item item = (Item)request.getAttribute("item");
%>

<dspace:layout titlekey="jsp.tools.move-item.title">

   	<form action="<%=request.getContextPath()%>/tools/edit-item" method="post">
   		
	  <table class="miscTable" align="center">
        <tr>
          <td class="evenRowEvenCol" colspan="2">
            <table>
              <tr>
                <td class="standard">
				  <small><strong><fmt:message key="jsp.tools.move-item.item.name.msg"/></strong></small>
			    </td>
			    <td class="standard">
				  <font color="#FF0000"><%=item.getMetadata("dc", "title", null, Item.ANY)[0].value%></font>
				</td>
			  </tr>
			  <tr>
				<td class="standard">
					<small><strong><fmt:message key="jsp.tools.move-item.collection.from.msg"/></strong></small>
				</td>
				<td class="standard">
				<select name="collection_from_id">
<%
        for (int i = 0; i < linkedCollections.length; i++)
        {
%>
            <option value="<%= linkedCollections[i].getID() %>"><%= linkedCollections[i].getMetadata("name") %></option>
<%
        }
%>
				</select>
				</td>
			  </tr>
			  <tr>
				<td class="standard">
					<small><strong><fmt:message key="jsp.tools.move-item.collection.to.msg"/></strong></small>
				</td>
				<td class="standard">
				<select name="collection_to_id">
<%
		//Later on find a away to display in a tree format with the linked one disabled?
        for (int i = 0; i < notLinkedCollections.length; i++)
        {
%>
            <option value="<%= notLinkedCollections[i].getID() %>"><%= notLinkedCollections[i].getMetadata("name") %></option>
<%
        }
%>
				</select>
			</td>
         </tr>
		 <tr>
       		<td class="standard"></td>
       		<td class="standard">
				<input type="submit" name="submit" value="<fmt:message key="jsp.tools.move-item.button"/>">
			</td>
         </tr>
        </table>
        </td>
      </tr>
     </table>
      <input type="hidden" name="action" value="<%=EditItemServlet.CONFIRM_MOVE_ITEM%>" />
      <input type="hidden" name="item_id" value="<%=item.getID() %>"> 
    </form>


</dspace:layout>
