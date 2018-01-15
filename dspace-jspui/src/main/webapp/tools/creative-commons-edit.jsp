<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show the user a license which they may grant or reject
  -
  - Attributes to pass in:
  -    submission.info  - the SubmissionInfo object
  -    license          - the license text to display
  -    cclicense.exists   - boolean to indicate CC license already exists
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@page import="org.dspace.app.webui.servlet.admin.EditItemServlet"%>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.license.CreativeCommons" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.license.CCLicense"%>
<%@ page import="java.util.Collection"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	Item item = (Item) request.getAttribute("item");

	Boolean lExists = (Boolean) request.getAttribute("cclicense.exists");
	boolean licenseExists = (lExists == null ? false : lExists.booleanValue());

	Collection<CCLicense> cclicenses = (Collection<CCLicense>) request.getAttribute("cclicense.licenses");

	String licenseURL = "";
	if (licenseExists)
		licenseURL = CreativeCommons.getLicenseURL(item);
%>

<dspace:layout navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer"
               titlekey="jsp.tools.creative-commons-edit.title" nocache="true">
<table cellspacing="8" cellpadding="24" class="pagecontent">
  <tr>
   <td>
    <h1><fmt:message key="jsp.tools.creative-commons-edit.heading1"/></h1>
				<form name="ccform" id="license_form" action="" method="post">


					<div class="row">
						<label class="col-md-2"><fmt:message
								key="jsp.submit.creative-commons.license" /></label> <span
							class="col-md-8"> <select name="licenseclass_chooser"
							id="licenseclass_chooser" class="form-control">
								<option
									value="webui.Submission.submit.CCLicenseStep.select_change"><fmt:message
										key="jsp.submit.creative-commons.select_change" /></option>
								<%
									if (cclicenses != null) {
											for (CCLicense cclicense : cclicenses) {
								%>
								<option value="<%=cclicense.getLicenseId()%>"><%=cclicense.getLicenseName()%></option>
								<%
									}
										}
								%>
								<option value="webui.Submission.submit.CCLicenseStep.no_license"><fmt:message
										key="jsp.submit.creative-commons.no_license" /></option>
						</select>
						</span>
					</div>
					<%
						if (licenseExists) {
					%>
					<div class="row" id="current_creativecommons">
						<label class="col-md-2"><fmt:message
								key="jsp.submit.creative-commons.license.current" /></label> <span
							class="col-md-8"> <a href="<%=licenseURL%>"><%=licenseURL%></a>
						</span>
					</div>
					<%
						}
					%>
					<div style="display: none;" id="creativecommons_response"></div>
					<br /> 
					
					<input type="hidden" name="item_id" value='<%=request.getParameter("item_id")%>'/> 
					<input type="hidden" name="cc_license_url" value="<%=licenseURL%>" />
					<input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_CC %>"/>
		            <input class="btn btn-default" type="submit" name="submit_cancel_cc" value="<fmt:message key="jsp.tools.general.cancel"/>" />
					<input class="btn btn-primary" type="submit" name="submit_change_cc" value="<fmt:message key="jsp.tools.general.update"/>" />
				</form>
			</td>
  </tr>
</table>

    <script type="text/javascript">
<!--
jQuery("#licenseclass_chooser").change(function() {
    var make_id = jQuery(this).find(":selected").val();
    var request = jQuery.ajax({
        type: 'GET',
        url: '<%=request.getContextPath()%>/json/creativecommons?license=' + make_id
    });
    request.done(function(data){
    	jQuery("#creativecommons_response").empty();
    	var result = data.result;
        for (var i = 0; i < result.length; i++) {
            var id = result[i].id;            
            var label = result[i].label;
            var description = result[i].description;
            var htmlCC = " <div class='form-group'><span class='help-block' title='"+description+"'>"+label+"&nbsp;<i class='glyphicon glyphicon-info-sign'></i></span>"
            var typefield = result[i].type;
            if(typefield=="enum") {            	
            	jQuery.each(result[i].fieldEnum, function(key, value) {
            		htmlCC += "<label class='radio-inline' for='"+id+"-"+key+"'>";
            		htmlCC += "<input placeholder='"+value+"' type='radio' id='"+id+"-"+key+"' name='"+id+"_chooser' value='"+key+"' required/>"+value+ "</label>";
            	});
            }
            htmlCC += "</div>";
            jQuery("#creativecommons_response").append(htmlCC);                
        }
        
        jQuery("#current_creativecommons").hide();
        jQuery("#creativecommons_response").show();
    });
});

//-->
</script>
</dspace:layout>
