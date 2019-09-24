<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c' %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir" %>

<dspace:layout titlekey="jsp.feedback.form.title">
    <h1><fmt:message key="jsp.feedback.form.title"/></h1>
    <p><fmt:message key="jsp.feedback.form.text1"/></p>


    <c:if test="${not empty message}">
        <div class="alert alert-${messageClass}" role="alert">
            ${message}
        </div>
    </c:if>

    <form action="<%= request.getContextPath() %>/feedback" method="post" class="form-horizontal">
            <div class="form-group">
                <label for="email" class="col-sm-2 control-label"><fmt:message key="jsp.feedback.form.email"/></label>
                <div class="col-sm-10">
                    <input type="text" name="email" id="email" size="50" value="${email}" class="form-control"/>
                </div>
            </div>
            <div class="form-group">
                <label for="feedback" class="col-sm-2 control-label"><fmt:message key="jsp.feedback.form.comment"/></label>
                <div class="col-sm-10">
                    <textarea name="feedback" id="feedback" rows="6" cols="50" class="form-control">${feedback}</textarea></div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-4 col-sm-6 g-recaptcha" data-sitekey="${recaptchaPublicKey}"></div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <input type="submit" name="submit"  class="btn btn-default" value="<fmt:message key="jsp.feedback.form.send"/>"/>
                </div>
            </div>
    </form>
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</dspace:layout>
