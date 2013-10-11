/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.ItemsResource;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class ItemsResourceServlet extends HttpServlet{
    
    private Item mockedItem, mockedItem2;
    private ItemsResource ir;
    
    private final Logger log = Logger.getLogger(ItemsResourceServlet.class);
    
    @Override
    public void init(ServletConfig config){
        mockedItem = mock(Item.class);
        when(mockedItem.getID()).thenReturn(1);
        when(mockedItem.isArchived()).thenReturn(true);
        when(mockedItem.isWithdrawn()).thenReturn(false);
        when(mockedItem.isDiscoverable()).thenReturn(true);
        when(mockedItem.getName()).thenReturn("test");
        
        DCValue[] value = new DCValue[2];
        value[0] = new DCValue(); value[1] = new DCValue();
        value[0].schema = "dc"; value[0].element = "contributor"; value[0].qualifier = "author";
        value[0].value = "Testi Testaaja";
        value[1].schema = "dc"; value[1].element = "date"; value[1].qualifier = "issued";
        value[1].value = "2013";
        
        when(mockedItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY)).thenReturn(value);
        
        mockedItem2 = mock(Item.class);
        when(mockedItem2.getID()).thenReturn(2);
        when(mockedItem2.isArchived()).thenReturn(true);
        when(mockedItem2.isWithdrawn()).thenReturn(false);
        when(mockedItem2.isDiscoverable()).thenReturn(true);
        when(mockedItem2.getName()).thenReturn("test2");
        
        DCValue[] value2 = new DCValue[2];
        value2[0] = new DCValue(); value2[1] = new DCValue();
        value2[0].schema = "dc"; value2[0].element = "contributor"; value2[0].qualifier = "author";
        value2[0].value = "Testi2 Testaaja2";
        value2[1].schema = "dc"; value2[1].element = "date"; value2[1].qualifier = "issued";
        value2[1].value = "2010";
        
        when(mockedItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY)).thenReturn(value);
        
        Item[] items = new Item[2];
        items[0] = mockedItem; items[1] = mockedItem2;
        
        ir = new ItemsResource(items, 1);
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        if(req.getPathInfo().equals("/xml")){
            xmlTest(resp);
        }else if(req.getPathInfo().equals("/json")){
            jsonTest(resp);
        }
    }
    
    public void xmlTest(HttpServletResponse resp) throws IOException{
        PrintWriter out = resp.getWriter();
        try{
            out.write(ir.toXml().getText());
        }catch(Exception ex){
            log.log(Priority.INFO, ex);
        }
    }
    
    public void jsonTest(HttpServletResponse resp) throws IOException{
        PrintWriter out = resp.getWriter();
        try{
            out.write(ir.toJson());
        }catch(Exception ex){
            log.log(Priority.INFO, ex);
        }
    }    
}
