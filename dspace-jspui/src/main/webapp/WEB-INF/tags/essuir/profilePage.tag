<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="language" rtexprvalue="true" required="true" type="java.lang.String"
              description="Language" %>

<%@ attribute name="chair" rtexprvalue="true" required="true" type="org.dspace.eperson.ChairEntity"
              description="User's chair." %>

<%@ attribute name="facultyList" rtexprvalue="true" required="true" type="java.util.List"
              description="Faculty list." %>

<%@ attribute name="chairListJson" rtexprvalue="true" required="true" type="java.lang.String"
              description="Faculty list." %>

<%@ attribute name="isRegisterPage" rtexprvalue="true" required="true" type="java.lang.Boolean"
              description="Is register page." %>

<%@ attribute name="isEditUserPage" rtexprvalue="true" required="true" type="java.lang.Boolean"
              description="Is edit user page." %>

<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tfirst_name"><fmt:message
            key="jsp.register.profile-form.fname.field"/></label>
    <div class="col-md-3">
        <input class="form-control" type="text" name="firstname" id="tfirst_name" size="40" value="${firstName}"/>
    </div>
</div>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tlast_name"><fmt:message
            key="jsp.register.profile-form.lname.field"/></label>
    <div class="col-md-3"><input class="form-control" type="text" name="lastname" id="tlast_name" size="40" value="${lastName}"/></div>
</div>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tphone"><fmt:message
            key="jsp.register.profile-form.phone.field"/></label>
    <div class="col-md-3">
        <input class="form-control" type="text" name="phone" id="tphone" size="40" maxlength="32" value="${phone}"/>
    </div>
</div>

<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tlanguage"><strong><fmt:message
            key="jsp.register.profile-form.language.field"/></strong></label>
    <div class="col-md-3">
        <select class="form-control" name="language" id="tlanguage">
            <c:forEach items="${supportedLocales}" var="supportedLocale">
                <c:set var="selected" value=""/>
                <c:if test="${language == supportedLocale.toString()}">
                    <c:set var="selected" value="selected = \"selected\""/>
                </c:if>

                <option ${selected} value="${supportedLocale.toString()}">${supportedLocale.getDisplayName(sessionLocale)}</option>
            </c:forEach>
        </select>
    </div>
</div>
<script type="text/javascript" src="../static/js/linkedselect.js"></script>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="faculty"><fmt:message
            key="jsp.dspace-admin.eperson.general.faculty"/></label>
    <div class="col-md-3">
        <select class="form-control" name="faculty" id="faculty">
            <option value="0"></option>
            <c:set var="facultyId" value="${chair == null ? -1 : chair.facultyEntityId}"/>
            <c:forEach items="${facultyList}" var="facultySelectEntity">
                <c:set var="facultySelected" value=""/>
                <c:if test="${facultyId == facultySelectEntity.id}">
                    <c:set var="facultySelected" value="selected = \"selected\""/>
                </c:if>
                <option value = '${facultySelectEntity.id}' ${facultySelected}>${facultySelectEntity.name}</option>
            </c:forEach>
        </select>
    </div>
</div>


<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="chair_id"><fmt:message
            key="jsp.dspace-admin.eperson.general.chair"/></label>

    <div class="col-md-3">
        <select class="form-control" name="chair_id" id="chair_id"></select>
        <c:set var="facultyId" value="${chair == null ? -1 : chair.id}"/>
        <script type="text/javascript">
            var syncList1 = new syncList();
            syncList1.dataList = ${chairListJson};
            syncList1.selectedId = ${facultyId};
            syncList1.sync("faculty", "chair_id");
        </script>
    </div>
</div>

<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tposition">
        <fmt:message key="jsp.dspace-admin.eperson.general.position"/>
    </label>
    <div class="col-md-3">
        <input class="form-control" name="position" id="tposition" size="24" value="${position}"/>
    </div>
</div>

<c:if test="${not isEditUserPage}">
    <c:if test="${not isRegisterPage}">
        <p class="alert"><fmt:message key="jsp.register.edit-profile.info5"/></p>
    </c:if>
    <div class="form-group">
        <label class="col-md-offset-3 col-md-2 control-label" for="tpassword"><fmt:message key="jsp.register.edit-profile.pswd.field"/></label>
        <div class="col-md-3">
            <input class="form-control" type="password" name="password" id="tpassword" />
        </div>
    </div>

    <div class="form-group">
        <label class="col-md-offset-3 col-md-2 control-label" for="tpassword_confirm"><fmt:message key="jsp.register.edit-profile.confirm.field"/></label>
        <div class="col-md-3">
            <input class="form-control" type="password" name="password_confirm" id="tpassword_confirm" />
        </div>
    </div>
</c:if>
