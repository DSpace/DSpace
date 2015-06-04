/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.util.Map;

import org.dspace.app.xmlui.aspect.administrative.controlpanel.AbstractControlPanelTab;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;

public class ControlPanelJavaTab extends AbstractControlPanelTab {


	@Override
	public void addBody(Map objectModel, Division div) throws WingException {


        // ufal
        List ufaladd = div.addList("LINDAT_Utilities");
        ufaladd.setHead("LINDAT Utilities");
                
    	ufaladd.addLabel("Server uptime");
        String uptime = 
        		cz.cuni.mff.ufal.Info.get_proc_uptime();
    	ufaladd.addItem(uptime);

    	ufaladd.addLabel("JVM uptime");
        String jvm_uptime = 
        		cz.cuni.mff.ufal.Info.get_jvm_uptime();
    	ufaladd.addItem( jvm_uptime );

    	ufaladd.addLabel("JVM start time");
    	ufaladd.addItem( 
    			cz.cuni.mff.ufal.Info.get_jvm_startime() );
    	ufaladd.addLabel("JVM version");
    	ufaladd.addItem(
    			cz.cuni.mff.ufal.Info.get_jvm_version() );
    	
    	ufaladd.addLabel("Build time");
		ufaladd.addItem(
				cz.cuni.mff.ufal.Info.get_ufal_build_time() );

        ufaladd.addLabel("Hibernate #connections (global)");
        ufaladd.addItem(
            cz.cuni.mff.ufal.Info.get_global_connections_count() );

        ufaladd.addLabel("Hibernate #sessions opened (so far)");
        ufaladd.addItem(
            cz.cuni.mff.ufal.Info.get_session_open_count() );

        ufaladd.addLabel("Hibernate #sessions closed (so far)");
        ufaladd.addItem(
            cz.cuni.mff.ufal.Info.get_session_close_count() );

    }
}

