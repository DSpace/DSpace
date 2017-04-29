/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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