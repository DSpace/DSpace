<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/static/webix/webix.css"/>
<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css"
      type="text/css"/>
<script src="<%= request.getContextPath() %>/static/webix/webix.js" type="text/javascript"></script>
<script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js"></script>
<script type='text/javascript'
        src="<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js"></script>
<script type='text/javascript'
        src="<%= request.getContextPath() %>/static/js/jquery/jquery.ui.datepicker-ru.js"></script>
<style>
    .ui-datepicker .ui-datepicker-title select {
        color: #000 !important;
    }
</style>
<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    String userEmail = "";

    if (user != null) userEmail = user.getEmail().toLowerCase();

    Boolean admin = (Boolean) request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate from = request.getParameter("fromDate") == null ? LocalDate.of(2010, 1, 1) : LocalDate.parse(request.getParameter("fromDate"), formatter);
        LocalDate to = request.getParameter("endDate") == null ? LocalDate.now() : LocalDate.parse(request.getParameter("endDate"), formatter);

%>
<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <div class="center-block">
        <div style="margin: 0 auto; width: 800px;">
            <form class="form-inline" style="margin-bottom: 20px;">
                <div class="form-group">
                    <label for="beginDate"><%= LocaleSupport.getLocalizedMessage(pageContext, "report.date-from") %>:</label>
                    <input type="text" id="beginDate" class="form-control" value="05.12.2010">
                </div>
                <div class="form-group">
                    <label for="endDate"><%= LocaleSupport.getLocalizedMessage(pageContext, "report.date-to") %>:</label>
                    <input type="text" id="endDate" class="form-control" value="01.01.2016">
                </div>
                <button type="button" class="btn btn-default" onclick="updateGrid();"><%= LocaleSupport.getLocalizedMessage(pageContext, "report.update") %></button>
            </form>

            <div id="reportTable" style="width:800px;"></div>
        </div>
    </div>
    <script>
        var depositorType = ["faculty", "chair", "person"];
        webix.ready(function () {
            grid = webix.ui({
                container: "reportTable",
                view: "treetable",
                columns: [
                    {
                        id: "name",
                        header: ["<%= LocaleSupport.getLocalizedMessage(pageContext, "report.depositor") %>", {content: "textFilter"}],
                        width: 600,
                        sort: "string",
                        template: function(obj, common, value, config) {
                            // console.log(obj.$level);
                            // var parent = grid.getItem(obj.$parent);
                            // var pname = "";
                            // if(parent) {
                            //     pname = parent.name + "//";
                            // }
                            return common.treetable(obj, common, value, config) + " <a href = \"/statistics/itemUploadingReport?from=" + $('#beginDate').val() + "&to=" + $('#endDate').val() + "&" +depositorType[obj.$level - 1] + "=" + obj.name + "&depositor=" + obj.name + "\">" + obj.name + "</a>";
                        }
                    },
                    {id: "submission_count", header: "<%= LocaleSupport.getLocalizedMessage(pageContext, "report.submissions-count") %>", width: 200, sort: "int"}
                ],
                autoheight: true,
                autowidth: true,
                on: {
                    onBeforeLoad: function () {
                        this.showOverlay("Loading...");
                    },
                    onAfterLoad: function () {
                        this.hideOverlay();
                    }
                },
                url: "/statistics/person?from=" + $('#beginDate').val() + "&to=" + $('#endDate').val(),
                datatype: "json"
            });
        });

        function parseDate(date) {
            var day = date.getDate();
            var month = date.getMonth() + 1;
            var year = date.getFullYear();
            if (day < 10) {
                day = '0' + day
            }

            if (month < 10) {
                month = '0' + month
            }
            return day + '.' + month + '.' + year;
        }

        $(document).ready(function () {
            var date = '<%= from %>';
            var endDate = '<%= to %>';
            $('#endDate').val(parseDate(new Date(endDate)));
            $('#beginDate').val(parseDate(new Date(date)));
        });

        $(function () {
            $.datepicker.setDefaults(
                    $.extend($.datepicker.regional["ru"])
            );
            $("#beginDate").datepicker({
                changeYear: true,
                yearRange: "2010:" + new Date().getFullYear()
            });
            $("#endDate").datepicker({
                changeYear: true,
                yearRange: "2010:" + new Date().getFullYear()
            });
        });

        function updateGrid() {
            var beginDate = $('#beginDate').val();
            var endDate = $('#endDate').val();
            grid.clearAll();
            grid.load("/statistics/person?from=" + beginDate + "&to=" + endDate);
        }

    </script>
</dspace:layout>
<% } else {
    org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
}%>