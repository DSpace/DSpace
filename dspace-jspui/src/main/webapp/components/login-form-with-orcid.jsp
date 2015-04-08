<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Component which displays a login form and associated information
  --%>
  
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	<div class="panel-body">
	<div class="row">
    <div class="col-md-6">
     <form name="loginform" class="form-horizontal" id="loginform" method="post" action="<%= request.getContextPath() %>/password-login">  
      <p><strong><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.components.login-form.newuser"/></a></strong></p>
	  <p><fmt:message key="jsp.components.login-form.enter"/></p>
		<div class="form-group">
            <label class="col-md-offset-1 col-md-4 control-label" for="tlogin_email"><fmt:message key="jsp.components.login-form.email"/></label>
            <div class="col-md-6">
            	<input class="form-control" type="text" name="login_email" id="tlogin_email" tabindex="1" />
            </div>
        </div>
        <div class="form-group">
            <label class="col-md-offset-1 col-md-4 control-label" for="tlogin_password"><fmt:message key="jsp.components.login-form.password"/></label>
            <div class="col-md-6">
            	<input class="form-control" type="password" name="login_password" id="tlogin_password" tabindex="2" />
            </div>
        </div>
        <div class="text-center">
        	<a class="btn btn-link" href="<%= request.getContextPath() %>/forgot"><fmt:message key="jsp.components.login-form.forgot"/></a>
        	<input type="submit" class="btn btn-success" name="login_submit" value="<fmt:message key="jsp.components.login-form.login"/>" tabindex="3" />
        </div>
      </form>
      </div>
      <div class="col-md-6">
      <h3 class="text-success"><fmt:message key="jsp.components.login-form.orcid-heading" /></h3>
      <p><fmt:message key="jsp.components.login-form.orcid-description"/></p>
      <p class="text-center">
      <a href="<%= request.getContextPath() %>/oauth-login">
      <button class="btn btn-default">
      	<fmt:message key="jsp.components.login-form.orcid-login"/>
      	<img src="<%= request.getContextPath() %>/image/orcid_64x64.png" title="ORCID Authentication" />
      </button></a></p>
      </div>
      </div>
      <script type="text/javascript">
		document.loginform.login_email.focus();
	  </script>
	</div>