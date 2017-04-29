/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.BitstreamResource;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dspace.content.Bitstream;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class BitstreamServlet extends HttpServlet{
    
    private Bitstream mockedBitstream;
    private BitstreamResource br;
    
    @Override
    public void init(ServletConfig config){
        mockedBitstream = mock(Bitstream.class, RETURNS_DEEP_STUBS);
        when(mockedBitstream.getID()).thenReturn(1);
        when(mockedBitstream.getName()).thenReturn("testi.pdf");
        when(mockedBitstream.getSize()).thenReturn(1337L);
        when(mockedBitstream.getUserFormatDescription()).thenReturn("");
        when(mockedBitstream.getDescription()).thenReturn("");
        when(mockedBitstream.getSource()).thenReturn("testi.pdf");
        
        br = new BitstreamResource(mockedBitstream, 1);
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        if(req.getPathInfo().equals("/xml")){
            xmlTest(resp);
        }else if(req.getPathInfo().equals("/json")){
            jsonTest(resp);
        }
    }

    private void xmlTest(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.write(br.get().getText());
    }

    private void jsonTest(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.write(br.toJson());
    }
}
