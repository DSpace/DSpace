<%--
  - complete.jsp
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
  - Submission complete message
  --%>

<dspace:layout locbar="nolink" navbar="off" parenttitle="Submit"
title="Submission Complete!">

    <jsp:include page="/submit/progressbar.jsp">
        <jsp:param name="step" value="<%= SubmitServlet.SUBMISSION_COMPLETE %>"/>
        <jsp:param name="stage_reached" value="<%= SubmitServlet.SUBMISSION_COMPLETE %>"/>
    </jsp:include>


    <H1>Submit: Submission Complete!</H1>
    
    <%-- FIXME    Probably should say something specific to workflow --%>
    <P>Your submission will now go through the workflow process designated for 
    the collection to which you are submitting.    You will receive e-mail
    notification as soon as your submission has become a part of the collection,
    or if for some reason there is a problem with your submission. You can also
    check on the status of your submission by going to the My DSpace page.</P>
    
    <P><A HREF="<%= request.getContextPath() %>/mydspace">Go to My DSpace</A></P>

</dspace:layout>
