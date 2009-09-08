<%--
  - Show contents of a etdunit
  -
  - Attributes:
  -   etdunit - etdunit to be edited
  -
  - Returns:
  -   cancel - if user wants to cancel
  -   change_name - alter name & redisplay
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ page import="org.dspace.content.Collection"   %>
<%@ page import="org.dspace.content.EtdUnit"   %>
<%@ page import="org.dspace.core.Utils" %>

<%
    Context context = UIUtil.obtainContext((HttpServletRequest)request);

    EtdUnit etdunit = (EtdUnit) request.getAttribute("etdunit");
    Collection[] collections  = (Collection[]) request.getAttribute("collections");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.tools.etdunit-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
	<h1><fmt:message key="jsp.tools.etdunit-edit.title"/> : <%=etdunit.getName()%> (id: <%=etdunit.getID()%>)</h1>
      </td>
      <td align="right" class="standard">
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") +\"#etduniteditor\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

  <center>
    <form name="collection" method="post" action="">
	<p><label for="tetdunit_name"><fmt:message key="jsp.tools.etdunit-edit.name"/></label><input name="etdunit_name" id="tetdunit_name" value="<%= Utils.addEntities(etdunit.getName()) %>" size="50"/></p>
   	    <h3><fmt:message key="jsp.tools.etdunit-edit.heading"/></h3>

        <input type="hidden" name="etdunit_id" value="<%=etdunit.getID()%>"/>
        <table>
          <tr>
            <td align="center"><strong><fmt:message key="jsp.tools.etdunit-edit.collection"/></strong><br/>
<%-- 
             <dspace:collectionlist collections="<%= Collection.findAll(context)%>"/>
--%>
            </td>
          </tr>
        </table>

        <br/>

        <p><input type="submit" name="submit_etdunit_update" value="<fmt:message key="jsp.tools.etdunit-edit.update.button"/>"  onclick="javascript:finishCollections();"/></p>
   </form>
  </center>
</dspace:layout>
