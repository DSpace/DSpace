<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - basic info for collection creation wizard
  -
  - attributes:
  -    collection - collection we're creating
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.admin.CollectionWizardServlet" %>
<%@ page import="org.dspace.content.Collection" %>

<%  Collection collection = (Collection) request.getAttribute("collection"); %>

<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.dspace-admin.wizard-basicinfo.title"
               nocache="true">


        <%-- <h1>Describe the Collection</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.wizard-basicinfo.title"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#wizard_description\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </h1>

    <form action="<%= request.getContextPath() %>/tools/collection-wizard" method="post" enctype="multipart/form-data">

				<div class="form-group"> 
	           		<label for="short_description"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.name"/></label>
            		<%-- <td><p class="submitFormLabel">Name:</p></td> --%>
	               <input class="form-control" type="text" name="name" size="50" id="tname" />
    			</div>        

<%-- Hints about table width --%>
            
            
                <%-- <td colspan="3" class="submitFormHelp">
                 Shown in list on community home page
                </td> --%>
				<div class="help-block">
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.shown"/>
                </div>
            	<div class="form-group">            
                	<%-- <td><p class="submitFormLabel">Short Description:</p></td> --%>
                	<label for="short_description"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.description"/></label>
                	<input class="form-control" type="text" name="short_description" size="50"/>
                </div>
           

				<div class="help-block">
	                <%-- HTML, shown in center of collection home page.  Be sure to enclose in &lt;P&gt; &lt;/P&gt; tags! --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.html1"/>
                </div>
            	<div class="form-group">
	                <%-- <td><p class="submitFormLabel">Introductory text:</p></td> --%>
            		<label for="introductory_text"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.intro"/></label>
                	<textarea class="form-control" name="introductory_text" rows="4" cols="50"></textarea>
				</div>
				
				<div class="help-block">
    				<%-- Plain text, shown at bottom of collection home page --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.plain"/>
                </div>
                <div class="form-group">
	                <%-- <td><p class="submitFormLabel">Copyright text:</p></td> --%>
    	            <label for="copyright_text"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.copyright"/></label>
        	        <textarea class="form-control" name="copyright_text" rows="3" cols="50"></textarea>
                </div>
            	     
            	<div class="help-block">    
            	     <%-- HTML, shown on right-hand side of collection home page.  Be sure to enclose in &lt;P&gt; &lt;/P&gt; tags! --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.html2"/>
                </div>
                
                <div class="form-group">
                	<%-- <td><p class="submitFormLabel">Side bar text:</p></td> --%>
                	<label for="side_bar_text"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.side"/></label>
                	<textarea class="form-control" name="side_bar_text" rows="4" cols="50"></textarea>
            	</div>


				<div class="help-block">
	                <%-- Licence that submitters must grant.  Leave this blank to use the default license. --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.license1"/>
				</div>
				<div class="form-group">
                <%-- <td><p class="submitFormLabel">License:</p></td> --%>
                	<label for="side_bar_text"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.license2"/></label>
                	<textarea class="form-control" name="license" rows="4" cols="50"></textarea></td>
                </div>

				<div class="help-block">
	                <%-- Plain text, any provenance information about this collection.  Not shown on collection pages. --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.plain2"/>
				</div>
				<div class="form-group">
					<%-- <td><p class="submitFormLabel">Provenance:</p></td> --%>
					<label for="provenance_description"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.provenance"/></label>
					<textarea class="form-control" name="provenance_description" rows="4" cols="50"></textarea>
				</div>

				<div class="help-block">
                	<%-- Choose a JPEG or GIF logo for the collection home page.  Should be quite small. --%>
	                <fmt:message key="jsp.dspace-admin.wizard-basicinfo.choose"/>
				</div>
				<div class="form-group">
					<%-- <td><p class="submitFormLabel">Provenance:</p></td> --%>
					<label for="file"><fmt:message key="jsp.dspace-admin.wizard-basicinfo.logo"/></label>
					<input class="form-control" type="file" size="40" name="file"/>
				</div>                	
	
	<div class="col-md-6 pull-right btn-group">
		<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        <input type="hidden" name="collection_id" value="<%= ((Collection) request.getAttribute("collection")).getID() %>" />
        <input type="hidden" name="stage" value="<%= CollectionWizardServlet.BASIC_INFO %>" />
        <%-- <input type="submit" name="submit_next" value="Next &gt;"> --%>
        <input class="btn btn-primary pull-right col-md-6" type="submit" name="submit_next" value="<fmt:message key="jsp.dspace-admin.general.next.button"/>" />
   	</div>
      		
    </form>

</dspace:layout>
