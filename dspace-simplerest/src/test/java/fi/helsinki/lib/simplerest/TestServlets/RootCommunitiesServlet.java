/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.RootCommunitiesResource;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dspace.content.Community;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class RootCommunitiesServlet extends HttpServlet{
    
    private Community mockedCommunity;
    private RootCommunitiesResource rcr;
    private final Logger log = Logger.getLogger(CommunityServlet.class.getName());
    
    @Override
    public void init(ServletConfig config) throws ServletException{
        mockedCommunity = mock(Community.class);
        when(mockedCommunity.getID()).thenReturn(1);
        when(mockedCommunity.getName()).thenReturn("test");
        when(mockedCommunity.getType()).thenReturn(10);
        when(mockedCommunity.getMetadata("short_description")).thenReturn("testi kuvaus");
        when(mockedCommunity.getMetadata("introductory_text")).thenReturn("testi intro");
        when(mockedCommunity.getMetadata("copyright_text")).thenReturn("testi copyright");
        when(mockedCommunity.getMetadata("side_bar_text")).thenReturn("testi sidebar");
        when(mockedCommunity.getLogo()).thenReturn(null);
        
        Community[] communities = new Community[2];
        communities[0] = mockedCommunity; communities[1] = mockedCommunity;
        
        rcr = new RootCommunitiesResource(communities);
    }
    
    /**
     *
     * @param req
     * @param resp
     * @throws IOException
     */
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
        try {
            out.write(rcr.toXml().getText());
        } catch (Exception ex) {
            log.log(Level.INFO, null, ex);
        } 
    }

    private void jsonTest(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try{
            out.write(rcr.toJson());
        }catch(Exception ex) {
            log.log(Level.INFO, null, ex);
        }
    }
}
