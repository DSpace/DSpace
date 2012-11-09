<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Main My DSpace page
  -
  -
  - Attributes:
  -    mydspace.user:    current user (EPerson)
  -    workspace.items:  WorkspaceItem[] array for this user
  -    workflow.items:   WorkflowItem[] array of submissions from this user in
  -                      workflow system
  -    workflow.owned:   WorkflowItem[] array of tasks owned
  -    workflow.pooled   WorkflowItem[] array of pooled tasks
  --%>

<%@page import="org.dspace.services.ConfigurationService"%>
<%@page import="org.dspace.utils.DSpace"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.SupervisedItem" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>
<%@ page import="java.util.List" %>

<%
    EPerson user = (EPerson) request.getAttribute("mydspace.user");

    WorkspaceItem[] workspaceItems =
        (WorkspaceItem[]) request.getAttribute("workspace.items");

    WorkflowItem[] workflowItems =
        (WorkflowItem[]) request.getAttribute("workflow.items");

    WorkflowItem[] owned =
        (WorkflowItem[]) request.getAttribute("workflow.owned");

    WorkflowItem[] pooled =
        (WorkflowItem[]) request.getAttribute("workflow.pooled");
	
    Group [] groupMemberships =
        (Group []) request.getAttribute("group.memberships");

    SupervisedItem[] supervisedItems =
        (SupervisedItem[]) request.getAttribute("supervised.items");
    
    List<String> exportsAvailable = (List<String>)request.getAttribute("export.archives");
    
    // Is the logged in user an admin
    Boolean displayMembership = (Boolean)request.getAttribute("display.groupmemberships");
    boolean displayGroupMembership = (displayMembership == null ? false : displayMembership.booleanValue());
    ConfigurationService configurationService = new DSpace().getConfigurationService();
    boolean crisEnabled = configurationService.getPropertyAsType("cris.enabled", false);

    if (crisEnabled)
    {
%>
<c:set var="dspace.layout.head.last" scope="request">
    <script type="text/javascript"><!--

		var j = jQuery.noConflict();
    	var myrpstatus = new Object();
    	j(document).ready(function(){
    		j('#cris-rp-change-active').dialog({
    			autoOpen: false, modal: true, width: 750, minHeight: 350,
   				buttons: {
   					"<fmt:message key="jsp.mydspace.cris.rp-status-change.go"/>": 
   						function(){
   							j(window).attr('location','<%= request.getContextPath() %>'+myrpstatus.url);
   						},
   					"<fmt:message key="jsp.mydspace.cris.rp-status-change.inactive"/>": 
   						function(){
	   						myRP('hide');
	    					j(this).dialog("close");
   						},
					"<fmt:message key="jsp.mydspace.cris.rp-status-change.remove"/>": 
						function(){
							myRP('remove');
	    					j(this).dialog("close");
   						},
					"<fmt:message key="jsp.mydspace.cris.rp-status-change.keep-active"/>": 
						function(){
	   						j(this).dialog("close");
	   					}
   				}
    		});
    		j('#cris-rp-change-inactive').dialog({
    			autoOpen: false, modal: true, width: 750, minHeight: 350,
   				buttons: {
   					"<fmt:message key="jsp.mydspace.cris.rp-status-change.go"/>": 
   						function(){
   							j(window).attr('location','<%= request.getContextPath() %>'+myrpstatus.url);
   						},
  					"<fmt:message key="jsp.mydspace.cris.rp-status-change.active"/>": 
  						function(){
	  						myRP('activate');
	    					j(this).dialog("close");		
        				},
    				"<fmt:message key="jsp.mydspace.cris.rp-status-change.remove"/>": 
    					function(){
	    					myRP('remove');
	    					j(this).dialog("close");	
        				},
    				"<fmt:message key="jsp.mydspace.cris.rp-status-change.keep-inactive"/>": 
    					function(){
        					j(this).dialog("close");
        				}
        			} 
        	});
    		j('#cris-rp-change-undefined').dialog({
    			autoOpen: false, modal: true, width: 750, minHeight: 300,
   				buttons: {
        			"<fmt:message key="jsp.mydspace.cris.rp-status-change.create"/>": 
        				function(){
        					myRP('create');
        					j(this).dialog("close");
        				},
    				"<fmt:message key="jsp.mydspace.cris.rp-status-change.keep-undefined"/>": 
    					function(){
        					j(this).dialog("close");
        				}
        		} 
        	});
    		
    		var myRP = function(myaction){
	    		j.ajax( {
					url : "<%= request.getContextPath() %>/cris/rp/myRp.json",
					data: {
						"action" : myaction
					},
					success : function(data) {
						myrpstatus = data.myrp;
						if (data.myrp.url != null && data.myrp.active)
						{
							j('#cris-rp-status-value').html('<fmt:message key="jsp.mydspace.cris.rp-status-active" />');
							j('#cris-rp-status-value').addClass("cris-rp-status-active");
							j('#cris-rp-changestatus').off('click');
							j('#cris-rp-changestatus').on('click', function(){
								j('#cris-rp-change-active').dialog("open");
							});
						} 
						else if (data.myrp.url != null && !data.myrp.active)
						{
							j('#cris-rp-status-value').html('<fmt:message key="jsp.mydspace.cris.rp-status-inactive" />');
							j('#cris-rp-status-value').addClass("cris-rp-status-inactive");
							j('#cris-rp-changestatus').off('click');
							j('#cris-rp-changestatus').on('click', function(){
								j('#cris-rp-change-inactive').dialog("open");
							});
						}
						else 
						{
							j('#cris-rp-status-value').html('<fmt:message key="jsp.mydspace.cris.rp-status-undefined" />');
							j('#cris-rp-status-value').addClass("cris-rp-status-undefined");
							j('#cris-rp-changestatus').off('click');
							j('#cris-rp-changestatus').on('click', function(){
								j('#cris-rp-change-undefined').dialog("open");
							});
						}										
					}
	    		});
    		};
    		
    		myRP('status');
    	});
    -->
    </script>
</c:set>    
<% } %>
<dspace:layout titlekey="jsp.mydspace" nocache="true">

<table width="100%" border="0">
        <tr>
            <td align="left">
                <h1>
                    <fmt:message key="jsp.mydspace"/>: <%= user.getFullName() %>
                </h1>
            </td>
            <td align="right" class="standard">
                 <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#mydspace\"%>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>
<%
    if (crisEnabled)
    {
        %>
        
        <h2 class="cris-rp-status">
        	<fmt:message key="jsp.mydspace.cris.rp-status-label"/> 
        	<a href="#" id="cris-rp-changestatus"><span id="cris-rp-status-value" class="cris-rp-status-value"><fmt:message key="jsp.mydspace.cris.rp-status-loading"/></span>
        	<img class="jdyna-icon jdyna-icon-action" src="<%= request.getContextPath() %>/image/jdyna/edit.gif" /></a>
        </h2>

        <div id="cris-rp-change-active" class="cris-rp-changestatus-dialog" title="<fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-active.title"/>">
        	<p><fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-active.text"/></p>
        </div>
        <div id="cris-rp-change-inactive" class="cris-rp-changestatus-dialog" title="<fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-inactive.title"/>">
        	<p><fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-inactive.text"/></p>
        </div>
        <div id="cris-rp-change-undefined" class="cris-rp-changestatus-dialog" title="<fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-undefined.title"/>">
        	<p><fmt:message key="jsp.mydspace.cris.rp-status-change.dialog-undefined.text"/></p>
        </div>
<%        
	}
 %>

<%-- Task list:  Only display if the user has any tasks --%>
<%
    if (owned.length > 0)
    {
%>
    <h2><fmt:message key="jsp.mydspace.main.heading2"/></h2>

    <p class="submitFormHelp">
        <%-- Below are the current tasks that you have chosen to do. --%>
        <fmt:message key="jsp.mydspace.main.text1"/>
    </p>

    <table class="miscTable" align="center" summary="Table listing owned tasks">
        <tr>
            <th id="t1" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.task"/></th>
            <th id="t2" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.item"/></th>
            <th id="t3" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.subto"/></th>
            <th id="t4" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.subby"/></th>
            <th id="t5" class="oddRowEvenCol">&nbsp;</th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < owned.length; i++)
        {
            DCValue[] titleArray =
                owned[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = owned[i].getItem().getSubmitter();
%>
        <tr>
                <td headers="t1" class="<%= row %>RowOddCol">
<%
            switch (owned[i].getState())
            {

            //There was once some code...
            case WorkflowManager.WFSTATE_STEP1: %><fmt:message key="jsp.mydspace.main.sub1"/><% break;
            case WorkflowManager.WFSTATE_STEP2: %><fmt:message key="jsp.mydspace.main.sub2"/><% break;
            case WorkflowManager.WFSTATE_STEP3: %><fmt:message key="jsp.mydspace.main.sub3"/><% break;
            }
%>
                </td>
                <td headers="t2" class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
                <td headers="t3" class="<%= row %>RowOddCol"><%= owned[i].getCollection().getMetadata("name") %></td>
                <td headers="t4" class="<%= row %>RowEvenCol"><a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a></td>
                <!-- <td headers="t5" class="<%= row %>RowOddCol"></td> -->
                <td headers="t5" class="<%= row %>RowEvenCol">
                     <form action="<%= request.getContextPath() %>/mydspace" method="post">
                        <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                        <input type="hidden" name="workflow_id" value="<%= owned[i].getID() %>" />  
                        <input type="submit" name="submit_perform" value="<fmt:message key="jsp.mydspace.main.perform.button"/>" />  
                        <input type="submit" name="submit_return" value="<fmt:message key="jsp.mydspace.main.return.button"/>" />
                     </form> 
                </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }

    // Pooled tasks - only show if there are any
    if (pooled.length > 0)
    {
%>
    <h2><fmt:message key="jsp.mydspace.main.heading3"/></h2>

    <p class="submitFormHelp">
        <%--Below are tasks in the task pool that have been assigned to you. --%>
        <fmt:message key="jsp.mydspace.main.text2"/>
    </p>

    <table class="miscTable" align="center" summary="Table listing the tasks in the pool">
        <tr>
            <th id="t6" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.task"/></th>
            <th id="t7" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.item"/></th>
            <th id="t8" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.subto"/></th>
            <th id="t9" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.subby"/></th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < pooled.length; i++)
        {
            DCValue[] titleArray =
                pooled[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = pooled[i].getItem().getSubmitter();
%>
        <tr>
                    <td headers="t6" class="<%= row %>RowOddCol">
<%
            switch (pooled[i].getState())
            {
            case WorkflowManager.WFSTATE_STEP1POOL: %><fmt:message key="jsp.mydspace.main.sub1"/><% break;
            case WorkflowManager.WFSTATE_STEP2POOL: %><fmt:message key="jsp.mydspace.main.sub2"/><% break;
            case WorkflowManager.WFSTATE_STEP3POOL: %><fmt:message key="jsp.mydspace.main.sub3"/><% break;
            }
%>
                    </td>
                    <td headers="t7" class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
                    <td headers="t8" class="<%= row %>RowOddCol"><%= pooled[i].getCollection().getMetadata("name") %></td>
                    <td headers="t9" class="<%= row %>RowEvenCol"><a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a></td>
                    <td class="<%= row %>RowOddCol">
                        <form action="<%= request.getContextPath() %>/mydspace" method="post">
                            <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                            <input type="hidden" name="workflow_id" value="<%= pooled[i].getID() %>" />
                            <input type="submit" name="submit_claim" value="<fmt:message key="jsp.mydspace.main.take.button"/>" />
                        </form> 
                    </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even");
        }
%>
    </table>
<%
    }
%>

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
        <center>
            <table border="0" width="70%">
                <tr>
                    <td align="left">
                        <input type="submit" name="submit_new" value="<fmt:message key="jsp.mydspace.main.start.button"/>" />
                    </td>
                    <td align="right">
                        <input type="submit" name="submit_own" value="<fmt:message key="jsp.mydspace.main.view.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>

    <p align="center"><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.mydspace.main.link"/></a></p>

<%
    // Display workspace items (authoring or supervised), if any
    if (workspaceItems.length > 0 || supervisedItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>

    <h2><fmt:message key="jsp.mydspace.main.heading4"/></h2>

    <p><fmt:message key="jsp.mydspace.main.text4" /></p>

    <table class="miscTable" align="center" summary="Table listing unfinished submissions">
        <tr>
            <th class="oddRowOddCol">&nbsp;</th>
            <th id="t10" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.subby"/></th>
            <th id="t11" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.elem1"/></th>
            <th id="t12" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.elem2"/></th>
            <th id="t13" class="oddRowOddCol">&nbsp;</th>
        </tr>
<%
        if (supervisedItems.length > 0 && workspaceItems.length > 0)
        {
%>
        <tr>
            <th colspan="5">
                <%-- Authoring --%>
                <fmt:message key="jsp.mydspace.main.authoring" />
            </th>
        </tr>
<%
        }

        for (int i = 0; i < workspaceItems.length; i++)
        {
            DCValue[] titleArray =
                workspaceItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = workspaceItems[i].getItem().getSubmitter();
%>
        <tr>
            <td class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/workspace" method="post">
                    <input type="hidden" name="workspace_id" value="<%= workspaceItems[i].getID() %>"/>
                    <input type="submit" name="submit_open" value="<fmt:message key="jsp.mydspace.general.open" />"/>
                </form>
            </td>
            <td headers="t10" class="<%= row %>RowEvenCol">
                <a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a>
            </td>
            <td headers="t11" class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
            <td headers="t12" class="<%= row %>RowEvenCol"><%= workspaceItems[i].getCollection().getMetadata("name") %></td>
            <td headers="t13" class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/mydspace" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= workspaceItems[i].getID() %>"/>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.mydspace.general.remove" />"/>
                </form> 
            </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>

<%-- Start of the Supervisors workspace list --%>
<%
        if (supervisedItems.length > 0)
        {
%>
        <tr>
            <th colspan="5">
                <fmt:message key="jsp.mydspace.main.supervising" />
            </th>
        </tr>
<%
        }

        for (int i = 0; i < supervisedItems.length; i++)
        {
            DCValue[] titleArray =
                supervisedItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = supervisedItems[i].getItem().getSubmitter();
%>

        <tr>
            <td class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/workspace" method="post">
                    <input type="hidden" name="workspace_id" value="<%= supervisedItems[i].getID() %>"/>
                    <input type="submit" name="submit_open" value="<fmt:message key="jsp.mydspace.general.open" />"/>
                </form>
            </td>
            <td class="<%= row %>RowEvenCol">
                <a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a>
            </td>
            <td class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
            <td class="<%= row %>RowEvenCol"><%= supervisedItems[i].getCollection().getMetadata("name") %></td>
            <td class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/mydspace" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= supervisedItems[i].getID() %>"/>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.mydspace.general.remove" />"/>
                </form>  
            </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }
%>

<%
    // Display workflow items, if any
    if (workflowItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>
    <h2><fmt:message key="jsp.mydspace.main.heading5"/></h2>

    <table class="miscTable" align="center" summary="Table listing submissions in workflow process">
        <tr>
            <th id="t14" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.elem1"/></th>
            <th id="t15" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.elem2"/></th>
        </tr>
<%
        for (int i = 0; i < workflowItems.length; i++)
        {
            DCValue[] titleArray =
                workflowItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
%>
            <tr>
                <td headers="t14" class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
                <td headers="t15" class="<%= row %>RowEvenCol">
                   <form action="<%= request.getContextPath() %>/mydspace" method="post">
                       <%= workflowItems[i].getCollection().getMetadata("name") %>
                       <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                       <input type="hidden" name="workflow_id" value="<%= workflowItems[i].getID() %>" />
                   </form>   
                </td>
            </tr>
<%
      row = (row.equals("even") ? "odd" : "even" );
    }
%>
    </table>
<%
  }

  if(displayGroupMembership && groupMemberships.length>0)
  {
%>
    <h2><fmt:message key="jsp.mydspace.main.heading6"/></h2>
    <ul>
<%
    for(int i=0; i<groupMemberships.length; i++)
    {
%>
    <li><%=groupMemberships[i].getName()%></li> 
<%    
    }
%>
	</ul>
<%
  }
%>

	<%if(exportsAvailable!=null && exportsAvailable.size()>0){ %>
	<h2><fmt:message key="jsp.mydspace.main.heading7"/></h2>
	<ol class="exportArchives">
		<%for(String fileName:exportsAvailable){%>
			<li><a href="<%=request.getContextPath()+"/exportdownload/"+fileName%>" title="<fmt:message key="jsp.mydspace.main.export.archive.title"><fmt:param><%= fileName %></fmt:param></fmt:message>"><%=fileName%></a></li> 
		<% } %>
	</ol>
	<%} %>
</dspace:layout>
