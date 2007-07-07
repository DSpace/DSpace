/*
 * StructureTest.java
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

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a class to test the capabilities of DRI's structural
 * elements: i.e. divs, paragraphs, lists, and tables.
 * 
 * This class is not internationalized because it is never intended
 * to be used in production. It is merely a tool to aid developers of
 * aspects and themes.
 * 
 * @author Scott Phillips
 */
public class StructureTest extends AbstractDSpaceTransformer
{
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent("Structural Test");
       
        pageMeta.addTrailLink(contextPath + "/","DSpace Home");
        pageMeta.addTrail().addContent("Structural Test");
    }

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division test = body.addDivision("test");
     
        test.setHead("Structural Tests");
        
        test.addPara("This is a series of tests that show various options and uses of DRI's structural elements: divisions, paragraphs, lists, and tables.");
        
        //////////////////////////////////////
        // Paragrpah test
        Division paraT = test.addDivision("para");
        paraT.setHead("1) Paragraph Tests");
        
        paraT.addPara("This is a simple paragraph");
        
        Para para = paraT.addPara();
        para.addContent("This is ");
        para.addHighlight("bold").addContent("not");
        para.addContent(" a ");
        para.addHighlight("bold").addHighlight("italic").addContent("simple");
        para.addContent(" paragraph.");
       
        /////////////////////////////////////
        // List test
        Division listT = test.addDivision("list");
        listT.setHead("2) List Tests");
        
        List list = listT.addList("simple");
        list.setHead("Simple list");
        list.addItem("one");
        list.addItem("two");
        list.addItem("three");
    
        list = listT.addList("labeled");
        list.setHead("Simple list ( with labels )");
        list.addLabel("uno");
        list.addItem("one");
        list.addLabel("dos");
        list.addItem("two");
        list.addLabel("threes");
        list.addItem("three");
        
        list = listT.addList("bulleted",List.TYPE_BULLETED);
        list.setHead("Bulleted list");
        list.addItem("one");
        list.addItem("two");
        list.addItem("three");
        
        list = listT.addList("ordered",List.TYPE_ORDERED);
        list.setHead("Ordered list");
        list.addItem("one");
        list.addItem("two");
        list.addItem("three");
        
        list = listT.addList("glossary",List.TYPE_GLOSS);
        list.setHead("Glossary list");
        list.addLabel("uno");
        list.addItem("one");
        list.addLabel("dos");
        list.addItem("two");
        list.addLabel("tres");
        list.addItem("three");
        
        // Nests
        
        list = listT.addList("simple-nest");
        list.setHead("Simple list ( nested )");
        list.addItem("one");
        list.addItem("two");
        List nest = list.addList("nest1");
        nest.setHead("Sub list point three");
        nest.addItem("three point one");
        nest.addItem("three point two");
        nest.addItem("three point three");
        list.addItem("four");
        
        list = listT.addList("labeled-nest");
        list.setHead("Simple list ( with labels and nested )");
        list.addLabel("uno");
        list.addItem("one");
        list.addLabel("dos");
        list.addItem("two");
        nest = list.addList("nest2");
        nest.addLabel("dos punto uno");
        nest.addItem("two point one");
        nest.addLabel("dos punto dos");
        nest.addItem("two point two");
        nest.addLabel("dos punto tres");
        nest.addItem("two point three");
        list.addLabel("threes");
        list.addItem("three");
        
        list = listT.addList("bulleted-nest",List.TYPE_BULLETED);
        list.setHead("Bulleted list ( nested )");
        list.addItem("one");
        list.addItem("two");
        nest = list.addList("nest3");
        nest.addItem("three point one");
        nest.addItem("three point two");
        nest.addItem("three point three");
        list.addItem("four");
        
        list = listT.addList("ordered-nest",List.TYPE_ORDERED);
        list.setHead("Ordered list ( nested )");
        list.addItem("one");
        list.addItem("two");
        nest = list.addList("nest4");
        nest.setHead("Sub list point three");
        nest.addItem("three point one");
        nest.addItem("three point two");
        nest.addItem("three point three");
        list.addItem("four");
        
        list = listT.addList("glossary-nest",List.TYPE_GLOSS);
        list.setHead("Glossary list ( nested )");
        list.addLabel("uno");
        list.addItem("one");
        list.addLabel("dos");
        list.addItem("two");
        nest = list.addList("nest5");
        nest.addLabel("dos punto uno");
        nest.addItem("two point one");
        nest.addLabel("dos punto dos");
        nest.addItem("two point two");
        nest.addLabel("dos punto tres");
        nest.addItem("two point three");
        list.addLabel("tres");
        list.addItem("three");
        
        ///////////////////////////////////////////////////
        // Table test
        Division tableT = test.addDivision("table");
        tableT.setHead("3) Table Tests");
        
        // Simple table
        Table table = tableT.addTable("table1",3,3);
        table.setHead("Table: simple");
        
        Row row = table.addRow();
        row.addCellContent("1.1");
        row.addCellContent("1.2");
        row.addCellContent("1.3");

        row = table.addRow();
        row.addCellContent("2.1");
        row.addCellContent("2.2");
        row.addCellContent("2.3");
        
        row = table.addRow();
        row.addCellContent("3.1");
        row.addCellContent("3.2");
        row.addCellContent("3.3");
        
        
        // Header vs data rows
        table = tableT.addTable("table1",4,3);
        table.setHead("Table: header vs data roles");
        
        row = table.addRow(Row.ROLE_HEADER);
        row.addCellContent("This whole");
        row.addCellContent("row is a");
        row.addCellContent("Header");

        row = table.addRow();
        row.addCellContent("2.1");
        row.addCellContent("2.2");
        row.addCellContent("2.3");
        
        row = table.addRow();
        row.addCellContent("3.1");
        row.addCell(Cell.ROLE_HEADER).addContent("3.2 - single cell header");
        row.addCellContent("3.3");
        
        row = table.addRow();
        row.addCellContent("4.1");
        row.addCellContent("4.2");
        row.addCellContent("4.3");
        
        // column and row spans
        table = tableT.addTable("table1",6,3);
        table.setHead("Table: column & row spans");
        
        row = table.addRow();
        row.addCellContent("1.1");
        row.addCellContent("1.2");
        row.addCellContent("1.3");
        
        row = table.addRow();
        row.addCell(null,null,0,3,null).addContent("2.1 - spans three columns");

        row = table.addRow();
        row.addCellContent("3.1");
        row.addCell(null,null,3,0,null).addContent("3.2 - spans three rows");
        row.addCellContent("3.3");
        
        row = table.addRow();
        row.addCellContent("4.1");
        //row.addCellContent("3.2"); // Should be missing
        row.addCellContent("4.3");

        row = table.addRow();
        row.addCellContent("5.1");
        //row.addCellContent("5.2"); // Should be missing
        row.addCellContent("5.3");
        
        row = table.addRow();
        row.addCellContent("6.1");
        row.addCellContent("6.2");
        row.addCellContent("6.3");
    }
}
