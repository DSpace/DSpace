<%--
  - authorize-advanced.jsp
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
  - Advanced policy tool - a bit dangerous, but powerful
  -
  - Attributes:
  -  collections - Collection [] all DSpace collections
  -  groups      - Group      [] all DSpace groups for select box
  - Returns:
  -  submit_advanced_clear - erase all policies for set of objects
  -  submit_advanced_add   - add a policy to a set of objects
  -  collection_id         - ID of collection containing objects
  -  resource_type         - type, "bitstream" or "item"
  -  group_id              - group that policy relates to
  -  action_id             - action that policy allows
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%@ page import="java.util.List"     %>
<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.Group"            %>

<%
    Group      [] groups     = (Group      []) request.getAttribute("groups"     );
    Collection [] collections= (Collection []) request.getAttribute("collections");
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-advanced.advanced"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.authorize-advanced.advanced"/></h1>

	<%-- <p>Allows you to do wildcard additions to and clearing
       of policies for types of content contained in a collection.
       Warning, dangerous - removing READ permissions from
       items will make them not viewable!  <dspace:popup page="/help/site-admin.html#advancedpolicies">More help...</dspace:popup></p> --%>
	<div><fmt:message key="jsp.dspace-admin.authorize-advanced.text"/> <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") +\"#advancedpolicies\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

    <form method="post" action="">

    <table class="miscTable" align="center" summary="Advanced policy manager">
        <tr>     
            <%-- <td>Collection:</td> --%>
            <th id="t1"><label for ="tcollection"><fmt:message key="jsp.dspace-admin.authorize-advanced.col"/></label></th>
            <td headers="t1">
                <select size="10" name="collection_id" id="tcollection">
                    <%  for(int i = 0; i < collections.length; i++ ) { %>
                            <option value="<%= collections[i].getID() %>"> <%= collections[i].getMetadata("name")%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>

        <tr>
            <%-- <td>Content Type:</td> --%>
            <th id="t2"><label for="tresource_type"><fmt:message key="jsp.dspace-admin.authorize-advanced.type"/></label></th>
            <td headers="t2">
                <select name="resource_type" id="tresource_type">
                    <%-- <option value="<%=Constants.ITEM%>">item</option>
                    <option value="<%=Constants.BITSTREAM%>">bitstream</option> --%>
                    <option value="<%=Constants.ITEM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type1"/></option>
                    <option value="<%=Constants.BITSTREAM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type2"/></option>
                </select>
            </td>
        </tr>

        <tr>     
            <%-- <td>Group:</td> --%>
            <th id="t3"><fmt:message key="jsp.dspace-admin.general.group-colon"/></th>
            <td headers="t3">
                <select size="10" name="group_id" id="tgroup_id">
                    <%  for(int i = 0; i < groups.length; i++ ) { %>
                            <option value="<%= groups[i].getID() %>"> <%= groups[i].getName()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>

        <tr>
            <%-- <tr><td>Action:</td> --%>
            <th id="t4"><label for="taction_id"><fmt:message key="jsp.dspace-admin.general.action-colon"/></label></th>
            <td headers="t4">
                <select name="action_id" id="taction_id">
                    <%  for( int i = 0; i < Constants.actionText.length; i++ ) { %>
                        <option value="<%= i %>">
                            <%= Constants.actionText[i]%>
                        </option>
                    <%  } %>
                </select>
            </td>
        </tr>
              
    </table>
    
    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_advanced_add" value="Add Policy"> --%>
                    <input type="submit" name="submit_advanced_add" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.add"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_advanced_clear" value="Clear Policies"> (warning: clears all policies for a given set of objects) --%>
                    <input type="submit" name="submit_advanced_clear" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.clear"/>" /></td> <td><fmt:message key="jsp.dspace-admin.authorize-advanced.warning"/>
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>

