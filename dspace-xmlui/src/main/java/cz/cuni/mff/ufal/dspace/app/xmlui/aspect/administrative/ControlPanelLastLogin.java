/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.eperson.EPerson;

/**
 * Show the users which have not logged in for a long time first.
 */
public class ControlPanelLastLogin extends AbstractControlPanelTab 
{
	
	private static Logger log = Logger.getLogger(ControlPanelLastLogin.class);
	private static final int MAX_TO_SHOW = 200;
	
	//
	//
	//

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException 
	{
		Division div_main = div.addDivision("last_logins", "well well-light");

		// header
		div_main.setHead(String.format("Last Login (showing only the first #%d)", MAX_TO_SHOW) );
		
		create_table( div_main );

    }

	
	private Table create_table(Division div) throws WingException 
	{
	    // this needs knot but we do not have so it will be redirected to manage epeople anyway
        String baseURL = String.format("%s/admin/epeople?submit_edit&epersonID=", 
                        contextPath );

        // table
		Table wftable = div.addTable("last_login_items", 1, 4);
		
		try {
			Row wfhead = wftable.addRow(Row.ROLE_HEADER);

			// table items - because of GUI not all columns could be shown
            wfhead.addCellContent("#");
            wfhead.addCellContent("Username");
			wfhead.addCellContent("Email");
			wfhead.addCellContent("Last login");
			
			EPerson[] epeople = EPerson.findAll(context, 0);
			final DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			
			Arrays.sort( epeople, new Comparator<EPerson>() {
			    @Override
			    public int compare(EPerson e1, EPerson e2) {
                    try {
                        String l1 = e1.getLoggedIn();
                        String l2 = e2.getLoggedIn();
                            if ( null == l1 && null == l2 )
                                return 0;
                            if ( null == l1 )
                                return -1;
                            if ( null == l2 )
                                return 1;
                        Date d1 = formatter.parse(l1);
                        Date d2 = formatter.parse(l2);
                        return d1.compareTo(d2);
                    } catch (ParseException e) {
                    }
                    return 0;
			    }			    
			});
			
			for ( int i = 0; i < epeople.length; ++i ) 
			{
			    Row r = wftable.addRow(Row.ROLE_DATA);
			    EPerson e = epeople[i];
                r.addCellContent(String.format("%d.", i + 1));
                // user/email
                r.addCell().addXref(baseURL + e.getID(), e.getFullName());
                r.addCellContent(e.getEmail());
                // logged in
                r.addCellContent(e.getLoggedIn());
			}
			
			
			
		}catch( IllegalArgumentException e1 ) {
			wftable.setHead( "No items - " + e1.getMessage() );
		}catch( Exception e2 ) {
			wftable.setHead( "Exception - " + e2.toString() );
		}
		
		return wftable;
	}
}






