/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.checks;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;

import cz.cuni.mff.ufal.dspace.IOUtils;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.AbstractControlPanelTab;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper;


/**
 * Add a section that shows last user logins.
 */
public class LoginControlCheck extends AbstractControlPanelTab {
	
	// constants
	// taken from https://lindat.mff.cuni.cz/secure/shib_test.pl
	//
	// the mapping is set using setenv for the script, but hardcoded here
	public static final Map<String,String> clarin_req_attr_our_mapping;
	
	static {
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("eppn", "eduPersonPrincipalName".toLowerCase()); 
		aMap.put("affiliation", "eduPersonScopedAffiliation".toLowerCase()); 
		aMap.put("cn", "cn"); 
		aMap.put("persistent-id", "eduPersonTargetedID".toLowerCase());
		clarin_req_attr_our_mapping = Collections.unmodifiableMap(aMap);
	}
	
	public static final String[] clarin_optional_attributes = {
			"givenName".toLowerCase(), 
			"sn",
			"ou",
			"on",
			"mail",
	}; 
	
	public static final String input_file_base = "authentication.log";
	

	private Request request = null;
	protected HtmlHelper html = null;	
	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {
		
		div = div.addDivision( this.getClass().getSimpleName(), "control_check  well well-light" );

		Request request = ObjectModelHelper.getRequest(objectModel);
		html = new HtmlHelper( div, web_link );		
				
		// select other files
		String[] all_latest_files = LoggingControlCheck.list_latest_log_files( "authentication.log" );
		html.file_chooser( all_latest_files );

		String input_file = null;
		String option = request.getParameter("extra");		
		if ( option != null ) {
			// security breach - but low
			input_file = LoggingControlCheck.absolute_log( option );
		} else {
			input_file = LoggingControlCheck.absolute_log( IOUtils.log_with_date( input_file_base, null ) );
		}
		
		BufferedReader safe_reader = null;
		try {
			safe_reader = IOUtils.safe_reader( input_file );
		} catch ( EOFException e ) {
			html.header( String.format("File: [%s] Warning: [%s]", 
					input_file_base, e.toString() ), 
					HtmlHelper.cls(
							HtmlHelper.header_class.WARNING
							) );
			return;
		} catch ( Exception e ) {
			html.exception( e.toString(), null );
			return;
		}
		
		// output warnings
		UserLogins user_logins = new UserLogins( safe_reader );
		if ( 0 < user_logins.warnings().size() ) {
			for ( String warning : user_logins.warnings() ) {
				html.failed();
				html.warning( warning );
			}
		}
		
		// output info
		html.header( String.format("File: [%s] User logins: [%d] successful: [%d]", 
						input_file_base, user_logins._logins.size(), user_logins._logins_successful ) );
		html.header1( String.format("Absolute file: [%s]", input_file) );

		
		for (UserLogin user_login : user_logins ) 
		{
			html.table( "users", true );
			String msg = user_login._id + " @ " + user_login._login_time + " => ";
			if ( user_login._successful )
				msg += "logged in successfully";
			else {
				html.failed();
				msg += "NOT logged in!!!";
			}
			html.table_header( new String[] { msg, null}, 
					user_login._successful ? HtmlHelper.header_class.OK : HtmlHelper.header_class.NOT_OK);
				
			java.util.Set<String> req_mapped = clarin_req_attr_our_mapping.keySet();
			java.util.Set<String> req_orig = new java.util.HashSet<String>(clarin_req_attr_our_mapping.values());
			java.util.List<String> opts = new ArrayList<String>( Arrays.asList( clarin_optional_attributes ) );
			for( Map.Entry<String, String> entry : user_login._headers.entrySet() ) {
				HtmlHelper.header_class rend = HtmlHelper.header_class.DEFAULT;
				String key = entry.getKey().toLowerCase();
				String value = entry.getValue().trim();
				if ( req_orig.contains(key) ) {
					rend = HtmlHelper.header_class.REQUIRED;
					if ( 0 < value.length() ) {
						req_orig.remove( key );
					}else {
						continue;
					}
				} else if(req_mapped.contains(key)){
					rend = HtmlHelper.header_class.REQUIRED;
					String orig = clarin_req_attr_our_mapping.get(key);
					if ( 0 < value.length() ) {
						req_orig.remove(orig);
						key = orig + " exported as " + key;
					}else {
						continue;
					}
				}
				if ( opts.contains(key) ) {
					rend = HtmlHelper.header_class.OPTIONAL;
					if ( 0 < value.length() ) {
						opts.remove( key );
					}else {
						continue;
					}
				}
				html.table_add( new String[] {
						key, value.replace(";", ";\n")
				}, rend );
			}
			// add attributes which are missing
			// they'll be listed  only at the end
			for ( String s : req_orig ) {
				html.table_add( new String[] { s, "!!!" }, HtmlHelper.header_class.REQUIRED_MISSING );
			}
			for ( String s : opts ) {
				html.table_add( new String[] { s, "!!!" }, HtmlHelper.header_class.OPTIONAL_MISSING );
			}
			
			html.space();
				
		} // for
	}
   
}

// OO friendly
//
//

class UserLogin 
{
	private static final String ID_HEADER = "header:mail";
	private static final String SUCCESSFUL_AUTHENTICATE_HEADER = "has been authenticated via ";
	private static final String SHIB_HEADER = "shib-identity-provider";

	String _login_time;
	String _id;
	HashMap<String,String> _headers;
	boolean _via_shib;
	boolean _successful;
	
	public UserLogin() {
		_id = null;
		_headers = new java.util.LinkedHashMap<String,String>();
		_login_time = null;
		_via_shib = false;
		_successful = false;
	}
	
	private boolean try_add_to_map( String header ) 
	{
		String values[] = header.split("=", 2);
		try {
			if(values[0].equals("")) values[0] = ".";
			//if(values[1].equals("")) values[1] = ".";
			String[] tmp= values[0].split(":");
			_headers.put( tmp[tmp.length-1], values[1] );
			
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public void add_header( String header ) {
		if ( null == _login_time ) {
			_login_time = header.substring( 0, 22 );
		}
		
		// add it to all headers
		try_add_to_map( header );

		// get id
		if( header.contains(ID_HEADER) ) {
			try {
				_id = header.split("=")[1];
			}catch(Exception e) {
			}
			return;
		}
		
		// really authenticated
		if( header.contains(SUCCESSFUL_AUTHENTICATE_HEADER) ) {
			_successful = true;
			return;
		}

		// really authenticated
		if( header.contains(SHIB_HEADER) ) {
			_via_shib = true;
			return;
		}
		
	} // add_header
}


class UserLogins implements Iterable<UserLogin> 
{
	// constants
	private static final String START_KEY = "Starting Shibboleth Authentication";
	private static final String END_KEY_OK = "has been authenticated";
	private static final String END_KEY_FAIL = "Unable to successfully authenticate";
	
	// vars
	public ArrayList< UserLogin > _logins;
	public ArrayList<String> _warnings;
	public int _logins_successful;
	
	// ctor - do all the stuff here
	public UserLogins( BufferedReader reader ) 
	{
		_logins = new ArrayList< UserLogin >();
		_warnings = new ArrayList< String >();
		_logins_successful = 0;
		
	
		try {
			String line = "";
			// case of exception
			boolean should_read_immediately = false;
			
			while( null != (line=reader.readLine()) ) 
			{
				// go to the beginning of new block
				if( !should_read_immediately && !line.contains(START_KEY) ) { 
					continue;
				}
				should_read_immediately = false;
				
				// read until exception/end/whatever
				UserLogin one_login = new UserLogin(); 
				// go after start key 
				reader.readLine();
				
				try {
					String header = null;
					while( (header=reader.readLine()) != null ) 
					{
						
						// ? leftover
						if(header == "") 
							break;
						
						// e.g., exception
						if( header.contains(START_KEY) ) {
							should_read_immediately = true;
							break;
						}

						// store it 
						one_login.add_header( header );
						
						// the end
						if( header.contains(END_KEY_FAIL) || header.contains(END_KEY_OK)  ) {
							break;
						}
						
					}
				
				} catch(IOException e) {
					_warnings.add( e.getMessage() );
				}
				
				_logins.add( one_login );
				if ( one_login._successful )
					++_logins_successful;
			}
		
		reader.close();
		
		} catch(Exception e) {
			_warnings.add( e.getMessage() );
			return;
		}
	
	}
	
	public java.util.List<String> warnings() {
		return _warnings;
	}
	
	public Iterator< UserLogin > iterator() {        
     return _logins.iterator();
 }


}
