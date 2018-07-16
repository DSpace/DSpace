<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - User profile editing form.
  -
  - This isn't a full page, just the fields for entering a user's profile.
  -
  - Attributes to pass in:
  -   eperson       - the EPerson to edit the profile for.  Can be null,
  -                   in which case blank fields are displayed.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="com.google.gson.GsonBuilder" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.I18nUtil" %>

<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="ua.edu.sumdu.essuir.cache.AuthorCache" %>
<%@ page import="ua.edu.sumdu.essuir.entity.AuthorLocalization" %>
<%@ page import="ua.edu.sumdu.essuir.entity.FacultyEntity" %>
<%@ page import="ua.edu.sumdu.essuir.utils.EssuirUtils" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>

<%
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    EPerson epersonForm = (EPerson) request.getAttribute("eperson");

    String lastName = "";
    String firstName = "";
    String phone = "";
    String language = "";
    int chairid = -1;
    String position = "";
    String orcid = "";


    if (epersonForm != null) {
        // Get non-null values
        lastName = epersonForm.getLastName();
        if (lastName == null) lastName = "";

        firstName = epersonForm.getFirstName();
        if (firstName == null) firstName = "";

        phone = epersonForm.getMetadata("phone");
        if (phone == null) phone = "";

        language = epersonForm.getMetadata("language");
        if (language == null) language = "";

        chairid = epersonForm.getChair();

        position = epersonForm.getPosition();
        if (position == null) position = "";

        orcid = AuthorCache.getAuthor(String.format("%s, %s", lastName, firstName))
                .map(EssuirUtils::findAuthor)
                .map(AuthorLocalization::getOrcid)
                .orElse("");
    }
    int faculty = (chairid == -1) ? -1 : EssuirUtils.getFacultyIdByChaidId(chairid);
%>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="first_name"><fmt:message
            key="jsp.register.profile-form.fname.field"/></label>

    <div class="col-md-3"><input class="form-control" type="text" name="first_name" id="tfirst_name" size="40"
                                 value="<%= Utils.addEntities(firstName) %>"/></div>
</div>
<div class="form-group">
    <%-- <td align="right" class="standard"><label for="tlast_name"><strong>Last name*:</strong></label></td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="tlast_name"><fmt:message
            key="jsp.register.profile-form.lname.field"/></label>

    <div class="col-md-3"><input class="form-control" type="text" name="last_name" id="tlast_name" size="40"
                                 value="<%= Utils.addEntities(lastName) %>"/></div>
</div>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tphone"><fmt:message
            key="jsp.register.profile-form.phone.field"/></label>

    <div class="col-md-3"><input class="form-control" type="text" name="phone" id="tphone" size="40" maxlength="32"
                                 value="<%= Utils.addEntities(phone) %>"/></div>
</div>
<div class="form-group">
    <label class="col-md-offset-3 col-md-2 control-label" for="tlanguage"><strong><fmt:message
            key="jsp.register.profile-form.language.field"/></strong></label>

    <div class="col-md-3">
        <select class="form-control" name="language" id="tlanguage">
            <%
                for (int i = supportedLocales.length - 1; i >= 0; i--) {
                    String lang = supportedLocales[i].toString();
                    String selected = "";

                    if (language.equals("")) {
                        if (lang.equals(I18nUtil.getSupportedLocale(request.getLocale()).getLanguage())) {
                            selected = "selected=\"selected\"";
                        }
                    } else if (lang.equals(language)) {
                        selected = "selected=\"selected\"";
                    }
            %>
            <option <%= selected %>
                    value="<%= lang %>"><%= supportedLocales[i].getDisplayName(UIUtil.getSessionLocale(request)) %>
            </option>
            <%
                }
            %>
        </select>
    </div>
</div>

<script type="text/javascript" src="../static/js/linkedselect.js"></script>

<div class="form-group">
    <%-- <td>Faculty*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="faculty"><fmt:message
            key="jsp.dspace-admin.eperson.general.faculty"/></label>
    <div class="col-md-3">
        <select class="form-control" name="faculty" id="faculty">
            <option value="0"></option>
            <%
                List<FacultyEntity> faculties = EssuirUtils.getFacultyList();
                for (FacultyEntity facultyEntity : faculties) {
                    out.println(String.format("<option value = '%s' %s>%s</option>", facultyEntity.getId(), (faculty == facultyEntity.getId() ? "selected" : ""), facultyEntity.getName()));
                }
            %>
        </select>
    </div>
</div>
<div class="form-group">
    <%-- <td>Chair*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="chair_id"><fmt:message
            key="jsp.dspace-admin.eperson.general.chair"/></label>

    <div class="col-md-3">
        <select class="form-control" name="chair_id" id="chair_id"></select>

        <script type="text/javascript">
            var syncList1 = new syncList;
            syncList1.dataList = <%= new GsonBuilder().create().toJson(EssuirUtils.getChairListByFaculties()) %>;
            syncList1.sync("faculty", "chair_id");
        </script>
    </div>
</div>

<div class="form-group">
    <%-- <td>Position*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="tposition"><fmt:message
            key="jsp.dspace-admin.eperson.general.position"/></label>

    <div class="col-md-3">
        <input class="form-control" name="position" id="tposition" size="24"
               value="<%=position == null ? "" : Utils.addEntities(position) %>"/>
    </div>
</div>


<div class="form-group">

    <label class="col-md-offset-3 col-md-2 control-label" for="orcid">ORCID</label>

    <div class="col-md-3">
        <input class="form-control" name="orcid" id="orcid" size="24"
               value="<%= orcid == null ? "" : Utils.addEntities(orcid) %>"/>
    </div>
</div>