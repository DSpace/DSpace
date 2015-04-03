/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import org.dspace.core.ConfigurationManager;
import org.junit.Test;

import javax.security.auth.login.Configuration;
import java.lang.reflect.Field;
import java.util.Properties;

public class ReportTest
{
    @Test
    public void testReportHandleResolutionStatistics() throws IllegalAccessException {
        String args[] = {"-s", "1"};
        try {
            DSpaceApi.load_dspace("../dspace/");
        }catch(Exception e) {
        }
        // this is revolting, but, works (for the moment)
        for (Field f : ConfigurationManager.class.getDeclaredFields()) {
            f.setAccessible(true);
            if ( f.getName().equals("properties") ) {
                ((Properties)f.get(null)).setProperty("dspace.url", "XXX");
            }
        }
        Report.main(args);
    }
}