<%--
  - edit-metadata-1.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>

<%--
  - Edit metadata form, page 1
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  --%>

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCPersonName" %>
<%@ page import="org.dspace.content.DCSeriesNumber" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    Item item = si.submission.getItem();

    final int lastNameWidth = 23;
    final int firstNamesWidth = 23;
    final int formWidth = 50;
%>

<dspace:layout locbar="off" navbar="off" title="Describe Your Item">

    <form action="<%= request.getContextPath() %>/submit#field" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.EDIT_METADATA_1 %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: Describe Your Item</H1>

        <P>Please fill in the requested information about your submission below.  In
        most browsers, you can use the tab key to move the cursor to the next input
        box or button, to save you having to use the mouse each time.
        <dspace:popup page="/help/index.html#describe2">(More Help...)</dspace:popup></P>
    
        <%-- HACK: a <center> tag seems to be the only way to convince certain --%>
        <%--       browsers to center the table. --%>
        <center>
            <table>

<%-- ================================================ --%>
<%--                Contributor.Author                --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
<%
    if (si.jumpToField != null && si.jumpToField.equals("contributor_author"))
    {
%>
                        <a name="field"></a>
<%
    }
%>
                        Enter the names of the authors of this item below.
                    </td>
                </tr>
<%-- Width hints used here to affect whole table --%>
                <tr>
                    <td width="40%">&nbsp;</td>
                    <td class="submitFormDateLabel" width="5%">Last name<br>e.g. <strong>Smith</strong></td>
                    <td class="submitFormDateLabel" width="5%">First name(s) + "Jr"<br> e.g. <strong>Donald Jr</strong></td>
                    <td width="40%">&nbsp;</td>
                </tr>
<%
    DCValue[] authors = item.getDC("contributor", "author", Item.ANY);
    int authorFieldCount = authors.length + 1;
    if (si.moreBoxesFor != null && si.moreBoxesFor.equals("contributor_author"))
    {
        authorFieldCount += 2;
    }

    for (int i = 0; i < authorFieldCount; i++)
    {
        DCPersonName dpn =
            (i < authors.length ? new DCPersonName(authors[i].value)
                                : new DCPersonName() );
%>
                <tr>
<%
        if (i == 0)
        {
%>
                    <td class="submitFormLabel">Authors</td>
<%
        }
        else
        {
%>
                    <td>&nbsp;</td>
<%
        }
%>
                    <td>
                        <input type=text name=contributor_author_last_<%= i %> size=<%= lastNameWidth %>
                            value="<%= dpn.getLastName() %>">
                    </td>
                    <td>
                        <input type=text name=contributor_author_first_<%= i %> size=<%= firstNamesWidth %>
                            value="<%= dpn.getFirstNames() %>">
                    </td>
<%
        if (i < authors.length)
        {
            // Put a "remove" button next to all filled-in values
%>
                    <td><input type=submit name=submit_contributor_author_remove_<%= i %> value="Remove This Author"></td>
<%
        }
        else if (i == authorFieldCount - 1)
        {
            // Put an "add more" button next to the last space (which is always
            // initially empty.)
%>
                    <td><input type=submit name=submit_contributor_author_more value="Add More"></td>
<%
        }
        else
        {
            // An empty space that isn't the last one
%>
                    <td>&nbsp;</td>
<%
        }
%>
                </tr>
<%
    }
%>

<%-- HACK: Using this line to give the browser hints as to the widths of cells --%>
                <tr>
                    <td>&nbsp;</td>
                    <td colspan=2 width=5%>&nbsp;</td>
                    <td>&nbsp;</td>
                </tr>

<%-- ================================================ --%>
<%--                     Main title                   --%>
<%-- ================================================ --%>

                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Enter the main title of the item.
                    </td>
                </tr>
<%
    // FIXME: (Maybe) Assumes a single main title
    DCValue[] titleArray = item.getDC("title", null, Item.ANY);
    String title = (titleArray.length > 0 ? titleArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Title</td>
                    <td colspan=2>
                        <input type=text name=title size=<%= formWidth %> value="<%= title %>">
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>

<%-- ================================================ --%>
<%--                   Alternative titles             --%>
<%-- ================================================ --%>

<%
    if (si.submission.hasMultipleTitles())
    {
%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
<%
        if (si.jumpToField != null && si.jumpToField.equals("title_alternative"))
        {
%>
                        <a name="field"></a>
<%
        }
%>
                        If the item has any alternative titles, please enter them below.
                    </td>
                </tr>
<%
        DCValue[] altTitles = item.getDC("title", "alternative", Item.ANY);
        int altTitleFieldCount = altTitles.length + 1;
        if (si.moreBoxesFor != null && si.moreBoxesFor.equals("title_alternative"))
        {
            altTitleFieldCount += 2;
        }
        

        for (int i = 0; i < altTitleFieldCount; i++)
        {
            String val = "";
            if (i < altTitles.length)
            {
                val = altTitles[i].value;
            }
%>
                <tr>
<%-- HACK: nowrap used since browsers do not act on "white-space" CSS property --%>
<%
            if (i == 0)
            {
%>
                    <td nowrap class="submitFormLabel">Other titles</td>
<%
            }
            else
            {
%>
                    <td>&nbsp;</td>
<%
            }
%>
                    <td colspan=2>
                        <input type=text name=title_alternative_<%= i %> size=<%= formWidth %> value="<%= val %>">
                    </td>
<%
            if (i < altTitles.length)
            {
                // Put a "remove" button next to all filled-in values
%>
                    <td><input type=submit name=submit_title_alternative_remove_<%= i %> value="Remove This Value"></td>
<%
            }
            else if(i == altTitleFieldCount - 1)
            {
                // Put an "add more" button next to the last space (which is always
                // initially empty.)
%>
                    <td><input type=submit name=submit_title_alternative_more value="Add More"></td>
<%
            }
            else
            {
                // An empty space that isn't the last one
%>
                    <td>&nbsp;</td>
<%
            }
%>
                </tr>
<%
        }
%>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>
<%
    }
%>

<%
    if (si.submission.isPublishedBefore())
    {
%>
<%-- ================================================ --%>
<%--                 date.issued                      --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Please give the date of previous publication or public distribution
                        below.  You can leave out the day and/or month if they aren't
                        applicable.
                    </td>
                </tr>
<% 
        DCValue[] dateIssuedArray = item.getDC("date", "issued", Item.ANY);
        DCDate dateIssued = new DCDate(
            (dateIssuedArray.length > 0 ? dateIssuedArray[0].value : ""));
%>
                <tr>
<%-- HACK: nowrap used since browsers do not act on "white-space" CSS property --%>
                    <td nowrap class="submitFormLabel">Date of Issue</td>
                    <td colspan=2 nowrap class="submitFormDateLabel">
                        Month:<select name="date_issued_month">
                            <option value="-1"<%= (dateIssued.getMonth() == -1 ? " SELECTED" : "") %>>(No month)</option>
<%
        for (int i = 1; i < 13; i++)
        {
%>
                            <option value=<%= i %><%= (dateIssued.getMonth() == i ? " SELECTED" : "" ) %>><%= DCDate.getMonthName(i) %></option>
<%
        }
%>
                        </select>
                        Day:<input type=text name=date_issued_day size=2 maxlength=2 value="<%= (dateIssued.getDay() > 0 ? String.valueOf(dateIssued.getDay()) : "" ) %>">
                        Year:<input type=text name=date_issued_year size=4 maxlength=4 value="<%= (dateIssued.getYear() > 0 ? String.valueOf(dateIssued.getYear()) : "" ) %>">
                    </td>
                    <td>
                        &nbsp;
                    </td>
                </tr>

                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>

<%-- ================================================ --%>
<%--                 publisher                        --%>
<%-- ================================================ --%>

                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Enter the name of the publisher of the previously issued instance of this item.
                    </td>
                </tr>
<%
    // FIXME: (Maybe) Assumes a single citation
    DCValue[] pubArray = item.getDC("publisher", null, Item.ANY);
    String publisher = (pubArray.length > 0 ? pubArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Publisher</td>
                    <td colspan=2>
                        <input type="text" name="publisher" size=<%= formWidth %> value="<%= publisher %>">
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--              identifier.citation                 --%>
<%-- ================================================ --%>

                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Enter the standard citation for the previously issued instance of this item.
                    </td>
                </tr>
<%
    // FIXME: (Maybe) Assumes a single citation
    DCValue[] citeArray = item.getDC("identifier", "citation", Item.ANY);
    String citation = (citeArray.length > 0 ? citeArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Citation</td>
                    <td colspan=2>
                        <input type=text name="identifier_citation" size=<%= formWidth %> value="<%= citation %>">
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>
<%
    }
%>


<%-- ================================================ --%>
<%--           relation.ispartofseries                --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
<%
    if (si.jumpToField != null && si.jumpToField.equals("relation_ispartofseries"))
    {
%>
                        <a name="field"></a>
<%
    }
%>
                        Enter the series and number assigned to this item by your community.
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td class="submitFormDateLabel">Series Name</td>
                    <td class="submitFormDateLabel">Report or Paper No.</td>
                    <td>&nbsp;</td>
                </tr>
<%
    DCValue[] seriesNumbers = item.getDC("relation", "ispartofseries", Item.ANY);
    int seriesNumberFieldCount = seriesNumbers.length + 1;

    if (si.moreBoxesFor != null && si.moreBoxesFor.equals("relation_ispartofseries"))
    {
        seriesNumberFieldCount += 2;
    }

    for (int i = 0; i < seriesNumberFieldCount; i++)
    {
        DCSeriesNumber sn =
            (i < seriesNumbers.length ? new DCSeriesNumber(seriesNumbers[i].value)
                                      : new DCSeriesNumber());
%>
                <tr>
<%
        if (i == 0)
        {
%>
                    <td class="submitFormLabel">Series/Report&nbsp;No.</td>
<%
        }
        else
        {
%>
                    <td>&nbsp;</td>
<%
        }
%>
                    <td>
                        <input type=text name=relation_ispartofseries_series_<%= i %> size=<%= lastNameWidth %>
                            value="<%= sn.getSeries() %>">
                    </td>
                    <td>
                        <input type=text name=relation_ispartofseries_number_<%= i %> size=<%= firstNamesWidth %>
                            value="<%= sn.getNumber() %>">
                    </td>
<%
        if (i < seriesNumbers.length)
        {
            // Put a "remove" button next to all filled-in values
%>
                    <td><input type=submit name=submit_relation_ispartofseries_remove_<%= i %> value="Remove This Value"></td>
<%
        }
        else if(i == seriesNumberFieldCount - 1)
        {
            // Put an "add more" button next to the last space (which is always
            // initially empty.)
%>
                    <td><input type=submit name=submit_relation_ispartofseries_more value="Add More"></td>
<%
        }
        else
        {
            // An empty space that isn't the last one
%>
                    <td>&nbsp;</td>
<%
        }
%>
                </tr>
<%
    }
%>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                Identifiers                       --%>
<%-- ================================================ --%>
<%
    // Names of each identifier type
    // FIXME: Maybe should draw from DC registry?
    Map identifierQualNames = new HashMap();
    identifierQualNames.put("govdoc", "Gov't Doc #");
    identifierQualNames.put("uri", "URI");
    identifierQualNames.put("isbn", "ISBN");
    identifierQualNames.put("issn", "ISSN");
    identifierQualNames.put("ismn", "ISMN");
    identifierQualNames.put("other", "Other");

    DCValue[] identifiers = item.getDC("identifier", Item.ANY, Item.ANY);
%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
<%
    if (si.jumpToField != null && si.jumpToField.equals("identifier"))
    {
%>
                        <a name="field"></a>
<%
    }
%>
                        If the item has any identification numbers or codes associated with
                        it, please enter the types and the actual numbers or codes below.
                    </td>
                </tr>
<%
    int identifierFieldCount = identifiers.length + 1;

    if (si.moreBoxesFor != null && si.moreBoxesFor.equals("identifier"))
    {
        identifierFieldCount += 2;
    }

    for (int i = 0; i < identifierFieldCount; i++)
    {
        String currentQual = "";
        String currentValue = "";

        if (i < identifiers.length)
        {
            currentQual = identifiers[i].qualifier;
            currentValue = identifiers[i].value;

            // Skip identifier.citation; handled elsewhere
            if (currentQual.equals("citation"))
            {
                break;
            }
        }
    
        if (i == 0)
        {
            // Only put "Identifiers" label next to first input box
%>
                <tr>
                    <td class="submitFormLabel">Identifiers</td>
<%
        }
        else
        {
%>
                    <td>&nbsp;</td>
<%
        }
%>
<%-- Qualifier pull-down --%>
                    <td colspan=2>
                        <select name="identifier_qualifier_<%= i %>">
<%
        Iterator quals = identifierQualNames.keySet().iterator();

        while (quals.hasNext())
        {
            String q = (String) quals.next();
%>
                            <option <%= (q.equals(currentQual) ? "SELECTED ": "" ) %> VALUE="<%= q %>"><%= identifierQualNames.get(q) %></option>
<%
        }
%>
                        </select>&nbsp;<input type=text name=identifier_value_<%= i %> size=34
                            value="<%= currentValue %>">
                    </td>
<%
        if (i < identifiers.length)
        {
            // Remove button next to filled-in values
%>
                    <td><input type=submit name=submit_identifier_remove_<%= i %> value="Remove This Value"></td>
<%
        }
        else if (i == identifierFieldCount - 1)
        {
            // "Add more" button on last line
%>
                    <td><input type=submit name=submit_identifier_more value="Add More"></td>
<%
        }
        else
        {
            // Empty space
%>
                    <td>&nbsp;</td>
<%
        }
%>
                </tr>
<%
    }
%>
                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                Type                              --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Select the type(s) of content you are submitting.  To
                        select more than one value in the list, you may have to
                        hold down the "CTRL" or "Shift" key.
                    </td>
                </tr>
<%
    String[] allTypes = {
                "Animation",
                "Article",
                "Book",
                "Book chapter",
                "Dataset",
                "Learning Object",
                "Image",
                "Image,3-D",
                "Map",
                "Musical Score",
                "Plan or blueprint",
                "Preprint",
                "Presentation",
                "Recording,acoustical",
                "Recording,musical",
                "Recording,oral",
                "Software",
                "Technical Report",
                "Thesis",
                "Video",
                "Working Paper",
                "Other"};

    DCValue[] typeArray = item.getDC("type", null, Item.ANY);
    // Make into a HashMap for easy access
    HashMap typeMap = new HashMap();
    for (int i = 0; i < typeArray.length; i++)
    {
        typeMap.put(typeArray[i].value, new Boolean(true));
    }
%>
                <tr>
                    <td class="submitFormLabel">Type</td>
                    <td colspan=2>
                        <select name="type" size=6 multiple>
<%
    for (int i = 0; i < allTypes.length; i++)
    {
%>
                            <option<%
        // option should be selected if it's in the DC
        if (typeMap.get(allTypes[i]) != null)
        { %> selected <% } %>><%= allTypes[i] %></option>
<%
    }
%>
                        </select>
                    </td>
                    <td>&nbsp;</td>
                </tr>

                <tr>
                    <td colspan=4>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                Language                          --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=4 class="submitFormHelp">
                        Select the language of the main content of the item.  If the language does not appear in the list below, please
                        select "Other".  If the content does not really have a language (for example, if it is a dataset or an image)
                        please select "N/A".
                    </td>
                </tr>
<%
    // FIXME: (Maybe) assume one language
    String[] defLanguages = {"en_US", "en", "es", "de", "fr", "it", "ja", "zh", "other", ""};
    DCValue[] langArray = item.getDC("language", "iso", Item.ANY);
    String lang = (langArray.length > 0 ? langArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Language</td>
                    <td colspan=2>
                        <select name=language_iso>
<% 
    for (int i = 0; i < defLanguages.length; i++)
    {
        DCLanguage tempLang = new DCLanguage(defLanguages[i]);
%>
                            <option value="<%= defLanguages[i] %>"<%= (lang.equals(defLanguages[i]) ? " SELECTED" : "" ) %>><%= tempLang.getDisplayName() %></option>
<%
    }
%>
                        </select>
                    </td>
                    <td>&nbsp;</td>
                </tr>
            </table>
        </center>
        
<%-- HACK:  Need a space - is there a nicer way to do this than <BR> or a --%>
<%--        blank <P>? --%>
        <P>&nbsp;</P>

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.EDIT_METADATA_1 %>>
        <center>
            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type=submit name=submit_prev value="&lt; Previous">
                    </td>
                    <td>
                        <input type=submit name=submit_next value="Next &gt;">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align=right>
                        <input type=submit name=submit_cancel value="Cancel/Save">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
