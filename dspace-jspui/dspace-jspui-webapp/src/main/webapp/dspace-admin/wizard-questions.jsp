<%--
  - wizard-questions.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
  - initial questions page for collection creation wizard
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

<%  Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue()); %>
    
<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.dspace-admin.wizard-questions.title"
               nocache="true">
    <%-- <h1>Describe the Collection</h1> --%>
<h1><fmt:message key="jsp.dspace-admin.wizard-questions.title"/></h1>

    <form action="<%= request.getContextPath() %>/tools/collection-wizard" method="post">
        <%--<p>Please check the boxes next to the statements that apply to the collection. --%>
        <div><fmt:message key="jsp.dspace-admin.wizard-questions.text"/>
          	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#createcollection\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
        <center>
            <table class="miscTable">
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="public_read" value="true"/>
                                <input type="checkbox" name="public_read" value="true" disabled="disabled" checked="checked"/>
                                <% } else { %>
                                <input type="checkbox" name="public_read" value="true" checked="checked"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>New items should be publicly readable</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check1"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="submitters" value="false" />
                                <input type="checkbox" name="submitters" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="submitters" value="true" checked="checked"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>Some users will be able to submit to this collection</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check2"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="workflow1" value="false" />
                                <input type="checkbox" name="workflow1" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow1" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check3"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="workflow2" value="false" />
                                <input type="checkbox" name="workflow2" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow2" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>accept/reject/edit metadata</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check4"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="workflow3" value="false" />
                                <input type="checkbox" name="workflow3" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="workflow3" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>The submission workflow will include an <em>edit metadata</em> step</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check5"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top">
                                <% if(!admin_button ) { %> <input type="hidden" name="admins" value="false" />
                                <input type="checkbox" name="admins" value="true" disabled="disabled"/>
                                <% } else { %>
                                <input type="checkbox" name="admins" value="true"/>
                                <% } %>
                                </td>
                                <%-- <td class="submitFormLabel" nowrap>This collection will have delegated collection administrators</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check6"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>                
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border="0">
                            <tr>
                                <td valign="top"><input type="checkbox" name="default.item" value="true"/></td>
                                <%-- <td class="submitFormLabel" nowrap>New submissions will have some metadata already filled out with defaults</td> --%>
                                <td class="submitFormLabel" nowrap="nowrap"><fmt:message key="jsp.dspace-admin.wizard-questions.check7"/></td>
                            </tr>
                        </table>
                    </td>
                </tr>
			</table>
		</center>
	
       <p>&nbsp;</p>

<%-- Hidden fields needed for servlet to know which collection and page to deal with --%>
        <input type="hidden" name="collection_id" value="<%= ((Collection) request.getAttribute("collection")).getID() %>" />
        <input type="hidden" name="stage" value="<%= CollectionWizardServlet.INITIAL_QUESTIONS %>" />

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;
                        
                    </td>
                    <td>
                        <input type="submit" name="submit_next" value="<fmt:message key="jsp.dspace-admin.general.next.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
