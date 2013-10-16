/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.MetadataFieldResource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class MetadataServlet extends HttpServlet{
    private MetadataField mockedMetadatafield;
    private MetadataSchema mockedMetadataschema;
    private MetadataFieldResource mfr;
    private Logger log;
    
    @Override
    public void init(){
        log = Logger.getLogger(MetadataServlet.class);
        mockedMetadataschema = mock(MetadataSchema.class);
        when(mockedMetadataschema.getName()).thenReturn("dckk");
        
        mockedMetadatafield = mock(MetadataField.class);
        when(mockedMetadatafield.getElement()).thenReturn("testElement");
        when(mockedMetadatafield.getQualifier()).thenReturn("testQualifier");
        when(mockedMetadatafield.getFieldID()).thenReturn(1);
        when(mockedMetadatafield.getScopeNote()).thenReturn("Description");
        
        mfr = new MetadataFieldResource(mockedMetadataschema, mockedMetadatafield, 1);
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        if(req.getPathInfo().equals("/xml")){
            xmlTest(resp);
        }else if(req.getPathInfo().equals("/json")){
            try {
                jsonTest(resp);
            } catch (SQLException ex) {
                log.log(Priority.FATAL, null, ex);
            }
        }
    }

    private void xmlTest(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.write(mfr.toXml().getText());
    }

    private void jsonTest(HttpServletResponse resp) throws SQLException, IOException {
        PrintWriter out = resp.getWriter();
        out.write(mfr.toJson());
    }
}
