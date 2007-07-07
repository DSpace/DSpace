/*
 * HTMLTest.java
 *
 * Version: $Revision: 1.4 $
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
package org.dspace.app.xmlui.aspect.xmltest;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;


/**
 * This is a class to test the capabilities of including HTML inside
 * a DRI document. This can be usefull for resources out side of a
 * developers control such as text files on disk or user supplied data.
 * 
 * This class is not internationalized because it is never intended
 * to be used in production. It is merely a tool to aid developers of
 * aspects and themes.
 * 
 * @author Scott Phillips
 */
public class HTMLTest extends AbstractDSpaceTransformer
{

	// The default string to include in test, may be overridden by the user.
	private static final String DEFAULT_HTML_STRING = "<p>This is a test of manakin's ability to render HTML fragments.</p>\n\n<p>Only a few tags are allowed such as: <b>bold</b>, <i>italic</i>, <u>underline</u>, and <a href=\"http://di.tamu.edu/\">link</a>.</p>\n\n<h2>This is a heading</h2>\n\nInvalid tags are treated as plain text: <invalid attribute=\"a\">this is invalid</invalid>\n\nAlso line breaks may be treated as a paragraphs when that action is specified.";
    
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent("HTML Test");
       
        pageMeta.addTrailLink(contextPath + "/","DSpace Home");
        pageMeta.addTrail().addContent("HTML Test");
    }

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	Request request = ObjectModelHelper.getRequest(objectModel);
    	String fragment = request.getParameter("fragment");
    	String[] options = request.getParameterValues("options");
    	
    	// If none present set the default HTML string
    	if (fragment == null || fragment.length() <= 0)
    		fragment = DEFAULT_HTML_STRING;
    	
    	boolean blankLines = false;
    	if (options != null && options[0].equals("blankLines"))
    		blankLines = true;
    	
        Division div = body.addInteractiveDivision("html-test", "", Division.METHOD_GET, "primary");
     
        div.setHead("HTML Test");
        
        div.addPara("This page tests Manakin's ability to handle HTML fragments, this ability is normally used to handle user-inputed text. There are two reasons for using this ability of including user supplied HTML fragments 1) it doesn't break the abstraction between themes and aspects, 2) it provides a safety mechanism preventing security vulnerabilities such as cross site scripting.");
        
        List form = div.addList("html-test",List.TYPE_FORM);
        TextArea fragmentField = form.addItem().addTextArea("fragment");
        fragmentField.setLabel("Fragment");
        fragmentField.setHelp("Enter free formed text, you may use <p>,<a>,<b>,<i>, or <img> tags.");
        fragmentField.setSize(15, 50);
        fragmentField.setValue(fragment);
        
        CheckBox optionsField = form.addItem().addCheckBox("options");
        optionsField.setLabel("Options");
        optionsField.addOption("blankLines", "Treat blank lines as paragraph breaks.");
        if (blankLines)
        	optionsField.setOptionSelected("blankLines");
        
        
        Button submit = form.addItem().addButton("submit");
        submit.setValue("Test HTML Rendering");
        
        Division test = div.addDivision("html-test-sample");
        test.setHead("Rendered Sample");
        
        test.addSimpleHTMLFragment(blankLines, fragment);
        
    }
}
