<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!-- Modal to handle e-mail writting -->
<div style="display:none;z-index: 9999;" id="emailDialog" title="<fmt:message key="share.search.email.title" />">

	<div id="actionResult" style="display: none;">
	</div>

	<form method="get" id="sendEmailForm" action="<%= request.getContextPath() %>/search/shareviaemail">
	
		<input type="hidden" id="urlToShare" name="urlToShare""></input>
		
		<table width="100%">
			<tr>
				<td>
					<div class="form-group">
			            <label for="senderName"><fmt:message key="share.search.email.form.name"/></label>
			           	<input class="form-control" name="senderName" id="senderName" size="24"/>
			        </div>
				</td>
				<td>
					<div class="form-group">
			            <label for="senderEmail"><fmt:message key="share.search.email.form.senderemail"/></label>
			           	<input class="form-control" name="senderEmail" id="senderEmail" size="24"/>
			        </div>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div class="form-group">
			            <label for="temail"><fmt:message key="share.search.email.form.email"/></label>
			           	<input class="form-control" name="email" id="temail" size="24"/>
			        </div>
				</td>
			</tr>
		</table>
	

		
		<div class="form-group">
            <label for="emailContent"><fmt:message key="share.search.email.form.description"/></label>
   	        <textarea class="form-control" id="emailContent" name="emailContent" rows="2" cols="50"></textarea>
        </div>
        
		<input class="btn btn-success" type="button" id="submitSend" name="submitSend" value="<fmt:message key="share.search.email.form.send"/>" />
		<input class="btn btn-default" type="reset" id="buttonReset" value="<fmt:message key="share.search.email.form.clear"/>" />
		<input class="btn btn-danger" type="button" id="closeModal" value="<fmt:message key="share.search.email.form.close"/>" />
	</form>
</div>