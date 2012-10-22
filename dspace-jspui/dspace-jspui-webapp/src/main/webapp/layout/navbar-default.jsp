<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Default navigation bar
--%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="java.util.Map" %>

<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;

    if (user != null)
    {
        navbarEmail = user.getEmail();
        if (navbarEmail.length() > 18)
        {
            navbarEmail = navbarEmail.substring(0, 17) + "...";
        }
    }
    
    // get the browse indices
    
	BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    BrowseInfo binfo = (BrowseInfo) request.getAttribute("browse.info");
    String browseCurrent = "";
    if (binfo != null)
    {
        BrowseIndex bix = binfo.getBrowseIndex();
        // Only highlight the current browse, only if it is a metadata index,
        // or the selected sort option is the default for the index
        if (bix.isMetadataIndex() || bix.getSortOption() == binfo.getSortOption())
        {
            if (bix.getName() != null)
    			browseCurrent = bix.getName();
        }
    }

%>

<%-- Search Box --%>
<form method="get" action="<%= request.getContextPath() %>/simple-search">

<%
    if (user != null)
    {
%>
  <p class="loggedIn"><fmt:message key="jsp.layout.navbar-default.loggedin">
      <fmt:param><%= navbarEmail %></fmt:param>
  </fmt:message>
    (<a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a>)</p>
<%
    }
%>
  <table width="100%" class="searchBox">
    <tr>
      <td>
        <table width="100%" border="0" cellspacing="0" >
          <tr>
            <td class="searchBoxLabel"><label for="tequery"><fmt:message key="jsp.layout.navbar-default.search"/></label></td>
          </tr>
          <tr>
            <td class="searchBoxLabelSmall" valign="middle" nowrap="nowrap">
              <%-- <input type="text" name="query" id="tequery" size="10"/><input type=image border="0" src="<%= request.getContextPath() %>/image/search-go.gif" name="submit" alt="Go" value="Go"/> --%>
              <input type="text" name="query" id="tequery" size="8"/><input type="submit" name="submit" value="<fmt:message key="jsp.layout.navbar-default.go"/>" />
              <br/><a href="<%= request.getContextPath() %>/advanced-search"><fmt:message key="jsp.layout.navbar-default.advanced"/></a>
<%
			if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
			{
%>        
              <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
<%
            }
%>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</form>

<%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
<table width="100%" border="0" cellspacing="2" cellpadding="2">
  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= (currentPage.endsWith("/index.jsp") ? "arrow-highlight" : "arrow") %>.gif" width="16" height="16"/>
    </td>

    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.layout.navbar-default.home"/></a>
    </td>
  </tr>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr>
    <td nowrap="nowrap" colspan="2" class="navigationBarSublabel"><fmt:message key="jsp.layout.navbar-default.browse"/></td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/community-list" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/community-list"><fmt:message key="jsp.layout.navbar-default.communities-collections"/></a>
    </td>
  </tr>


<%-- Insert the dynamic browse indices here --%>

<%
	for (int i = 0; i < bis.length; i++)
	{
		BrowseIndex bix = bis[i];
		String key = "browse.menu." + bix.getName();
	%>
		<tr class="navigationBarItem">
    		<td>
      			<img alt="" src="<%= request.getContextPath() %>/image/<%= ( browseCurrent.equals(bix.getName()) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    		</td>
    		<td nowrap="nowrap" class="navigationBarItem">
      			<a href="<%= request.getContextPath() %>/browse?type=<%= bix.getName() %>"><fmt:message key="<%= key %>"/></a>
    		</td>
  		</tr>
	<%	
	}
%>

<%-- End of dynamic browse indices --%>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr>
    <td nowrap="nowrap" colspan="2" class="navigationBarSublabel"><fmt:message key="jsp.layout.navbar-default.sign"/></td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/subscribe" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/mydspace" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.layout.navbar-default.users"/></a><br/>
      <fmt:message key="jsp.layout.navbar-default.users-authorized" />
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/profile" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a>
    </td>
  </tr>

<%
  if (isAdmin)
  {
%>  
  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/dspace-admin" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/></a>
    </td>
  </tr>
<%
  }
%>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/help" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\")%>"><fmt:message key="jsp.layout.navbar-default.help"/></dspace:popup>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/about" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="http://www.dspace.org/"><fmt:message key="jsp.layout.navbar-default.about"/></a>
    </td>
  </tr>

  <c:if test="${researcher_page_menu && !empty researcher}">
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  
  <tr>
    <td nowrap="nowrap" colspan="2" class="navigationBarSublabel"><fmt:message key="jsp.layout.navbar-hku.staffmode.title"/></td>
  </tr>

  <c:if test="${!empty addModeType && addModeType=='display'}">
  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/editDynamicData" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/cris/tools/rp/editDynamicData.htm?id=${researcher.id}&anagraficaId=${researcher.dynamicField.id}<c:if test='${!empty tabIdForRedirect}'>&tabId=${tabIdForRedirect}</c:if>"><fmt:message key="jsp.layout.navbar-hku.staff-mode.edit.primary-data"/></a>
    </td>
  </tr>  
  </c:if>
   <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/rebindItemsToRP" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/cris/tools/rp/rebindItemsToRP.htm?id=${researcher.id}"><fmt:message key="jsp.layout.navbar-hku.staff-mode.bind.items"/></a>
    </td>
  </tr>
   <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/help#ResearcherPages" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/help.jsp#ResearcherPages">Help</a>
    </td>
  </tr>
  </c:if>
  
  <% if (isAdmin) { %>
  <tr> 
  <td colspan="2">
	<c:if test="${!empty researcher}">
	
		
			<p><b>Staff no. ${researcher.staffNo} </b><br/>
			<br />
			record created at:
			${researcher.timeStampInfo.timestampCreated.timestamp} <br/>
			<br />
			last updated at:
			${researcher.timeStampInfo.timestampLastModified.timestamp}<br/>
			</p>
		
	
	</c:if>
	</td>
  </tr>
<% } %>


<c:if test="${grant_page_menu && !empty project}">


  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  
  <tr>
    <td nowrap="nowrap" colspan="2" class="navigationBarSublabel"><fmt:message key="jsp.layout.navbar-hku.staffmode.title"/></td>
  </tr>
  

  <c:if test="${!empty addModeType && addModeType=='display'}">
  
  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/editDynamicData" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/cris/tools/project/editDynamicData.htm?id=${project.id}&anagraficaId=${project.dynamicField.id}<c:if test='${!empty tabIdForRedirect}'>&tabId=${tabIdForRedirect}</c:if>"><fmt:message key="jsp.layout.navbar.entity.edit"/></a>
    </td>
  </tr>  
 </c:if>
  
   <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/help#Projects" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16"/>
    </td>
    <td nowrap="nowrap" class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/help.jsp#Projects">Help</a>
    </td>
  </tr>
 
 </c:if>


<c:if test="${!empty entity && (!empty addModeType && addModeType=='display')}">    
 <% if (!isAdmin) { %>
 <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
<% } %>	
 <tr> 
  <td colspan="2">
		
	    <c:forEach items="${tabList}" var="tabfornavigation">				
			
				<div id="cris-tabs-navigation-${tabfornavigation.shortName}" class="navigation-tabs" style="display: none">		

					<div id="menu-${tabfornavigation.shortName}" class="showMoreLessBox1-dark box">
						<h3 class="showMoreLessControlElement control ${tabfornavigation.id != tabId?"":"expanded"}">
						<img src="<%=request.getContextPath() %>/image/cris/btn_lite_expand.gif"  ${tabfornavigation.id != tabId?"":"class=\"hide\""}/>
						<img src="<%=request.getContextPath() %>/image/cris/btn_lite_collapse.gif" ${tabfornavigation.id != tabId?"class=\"hide\"":""} />
							${tabfornavigation.title}
						</h3>		
						<div class="collapsable expanded-content" ${tabfornavigation.id != tabId?"style=\"display: none;\"":""}>
						<div id="nav-sublocal">
						<ul>
						<div id="snavmenu-${tabfornavigation.shortName}">
							<div class="log">
							<img
								src="<%=request.getContextPath()%>/image/jdyna/indicator.gif"
			    				class="loader" />
			    			</div>
						</div>
						</ul>
						</div>
						</div>
					</div>
		

				</div>
			
		 </c:forEach>
	   
	</td>
  </tr> 
</c:if>
 
</table>
