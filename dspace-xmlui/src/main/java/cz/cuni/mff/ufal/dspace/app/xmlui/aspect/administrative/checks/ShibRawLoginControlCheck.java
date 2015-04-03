/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.checks;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.checks.ShibUserLogin;
import cz.cuni.mff.ufal.checks.ShibUserLogins;
import cz.cuni.mff.ufal.dspace.IOUtils;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.AbstractControlPanelTab;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper;


/**
 * Add a section that shows last user logins.
 */
public class ShibRawLoginControlCheck extends AbstractControlPanelTab {
	
	public static final String input_dir = ConfigurationManager.getProperty("lr","lr.shibboleth.log.path");
	public static final String default_log = ConfigurationManager.getProperty("lr","lr.shibboleth.log.defaultName");
	
	private Request request = null;
	protected HtmlHelper html = null;

	private static final Message title = message("xmlui.administrative.ControlPanel.option_shib_logins");
	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		div = div.addDivision( this.getClass().getSimpleName(), "control_check well well-light" );

		request = ObjectModelHelper.getRequest(objectModel);

		html = new HtmlHelper( div, web_link );
		
		// select other files
		String[] log_files = IOUtils.list_files( input_dir, default_log );
		if(log_files == null || log_files.length == 0){
			html.warning("No log files found, check your settings. Serching for "+default_log+" in "+input_dir);
		}

		html.file_chooser( log_files );
		
		String option = request.getParameter("extra");
		
		String input_file = null;
		
		if ( option != null && !option.equals("")) {
			input_file = new File(input_dir, option).toString();
		}else {
			input_file = new File(input_dir, default_log).toString();
		}		

		BufferedReader safe_reader = null;
		try {
			safe_reader = IOUtils.safe_reader( input_file );
		} catch ( EOFException e ) {
			html.header( String.format("File: [%s] Warning: [%s]", 
					default_log, e.toString() ), 
					HtmlHelper.cls(
							HtmlHelper.header_class.WARNING
							) );
			return;
		} catch ( Exception e ) {
			html.exception( e.toString(), null );
			return;
		}
		
		// output warnings
		ShibUserLogins user_logins = new ShibUserLogins( safe_reader );
		if ( 0 < user_logins.warnings().size() ) {
			for ( String warning : user_logins.warnings() ) {
				html.failed();
				html.warning( warning );
			}
		}
		
		// output info
		html.header( String.format("File: [%s] User logins: [%d] successful: [%d]", 
				default_log, user_logins._logins.size(), user_logins._logins_successful ) );
		html.header1( String.format("Absolute file: [%s]", input_file) );
		
		for (ShibUserLogin user_login : user_logins ) 
		{
			html.table( "users", true );
			String msg = user_login._id + " @ [" + user_login._ip + "][" + user_login._protocol + "]" 
					+ user_login._login_time + " => ";
				
			
			if ( user_login._successful )
				msg += "logged in successfully";
			else {
				html.failed();
				msg += "NOT logged in!!!";
			}
			html.table_header( new String[] { msg, null}, 
					user_login._successful ? HtmlHelper.header_class.OK : HtmlHelper.header_class.NOT_OK);
				
			for( String entry : user_login._headers ) {
				html.table_add( new String[] {
						"", entry
				}, HtmlHelper.header_class.REQUIRED_MISSING );
			}
			html.space();
				
		} // for
	}
	   
}



