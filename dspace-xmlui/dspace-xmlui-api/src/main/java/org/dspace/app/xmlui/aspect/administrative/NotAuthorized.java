/*
 * NotAuthorized.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;

/**
 * Display a generic error message saying the user can
 * not preform the requested action.
 * 
 * @author Scott phillips
 */
public class NotAuthorized extends AbstractDSpaceTransformer   
{	
	
	private static final Message T_title = 
		message("xmlui.administrative.NotAuthorized.title");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

	private static final Message T_trail = 
		message("xmlui.administrative.NotAuthorized.trail");
	
	private static final Message T_head = 
		message("xmlui.administrative.NotAuthorized.head");
	
	private static final Message T_para1a = 
		message("xmlui.administrative.NotAuthorized.para1a");

	private static final Message T_para1b = 
		message("xmlui.administrative.NotAuthorized.para1b");
	
	private static final Message T_para1c = 
		message("xmlui.administrative.NotAuthorized.para1c");
	
	private static final Message T_para2 = 
		message("xmlui.administrative.NotAuthorized.para2");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		String loginURL = contextPath+"/login";
		String feedbackURL = contextPath+"/feedback";
		
        Division main = body.addDivision("not-authorized","primary administrative");
		main.setHead(T_head);
		Para para1 = main.addPara();
		para1.addContent(T_para1a);
		para1.addXref(feedbackURL,T_para1b);
		para1.addContent(T_para1c);

		main.addPara().addXref(loginURL,T_para2);
		
	}
	
}
