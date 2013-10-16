/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.TestServlets;

import fi.helsinki.lib.simplerest.GroupResource;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;
import org.dspace.eperson.Group;
import static org.mockito.Mockito.*;

/**
 *
 * @author moubarik
 */
public class GroupServlet extends HttpServlet{
    
    private Group mockedGroup;
    private GroupResource gr;
    private static Logger log = Logger.getLogger(GroupServlet.class);
    
    @Override
    public void init(ServletConfig config){
        mockedGroup = mock(Group.class);
    }
}