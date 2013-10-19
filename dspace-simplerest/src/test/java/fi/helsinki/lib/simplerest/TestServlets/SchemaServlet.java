/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.MetadataSchemaResource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.MetadataSchema;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class SchemaServlet extends HttpServlet{
    private MetadataSchema mockedMetadataschema;
    private MetadataSchemaResource msr;
    private static Logger log = Logger.getLogger(SchemaServlet.class);
    
    @Override
    public void init(){
        mockedMetadataschema = mock(MetadataSchema.class);
        when(mockedMetadataschema.getSchemaID()).thenReturn(1);
        when(mockedMetadataschema.getName()).thenReturn("dckk");
        when(mockedMetadataschema.getNamespace()).thenReturn("http://kk.fi/dckk/");
        
        msr = new MetadataSchemaResource(mockedMetadataschema, 1);
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        if(req.getPathInfo().equals("/xml")){
            xmlTest(resp);
        }else if(req.getPathInfo().equals("/json")){
            try{
                jsonTest(resp);
            }catch(SQLException ex){
                log.log(Priority.INFO, ex);
            }
        }
    }

    private void xmlTest(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.write(msr.toXml().getText());
    }

    private void jsonTest(HttpServletResponse resp) throws IOException, SQLException {
        PrintWriter out = resp.getWriter();
        out.write(msr.toJson());
    }
    
}
