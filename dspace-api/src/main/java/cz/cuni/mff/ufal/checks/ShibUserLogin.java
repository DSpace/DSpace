/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.checks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class ShibUserLogin 
{
	private static final String SUCCESSFUL_AUTHENTICATE_HEADER = "new session created:";
	private static final String ID_HEADER = "Destination=\"";
	

	public String _login_time = null;
	public String _id = null;
	public String _ip = null;
	public String _protocol = null;
	public ArrayList<String> _headers;
	//Protocol(urn:oasis:names:tc:SAML:2.0:protocol) Address (89.177.48.194)
	Pattern _pattern = Pattern.compile("Protocol\\((.*)\\) Address \\(([^)]*)\\)");
	
	public boolean _successful = false;
	
	public ShibUserLogin() {
		_headers = new ArrayList<String>();
	}
	
	private boolean try_add_to_map( String header ) 
	{
		if ( header.contains("skipping") ) {
			_headers.add( header);
		}

		return true;
	}
	
	public void add_header( String header ) {
		
		// get id
		if( header.contains(ID_HEADER) && _id == null ) {
			try {
				_id = StringUtils.strip( header.split(ID_HEADER)[1].split(" ")[0], "\"" );
			}catch(Exception e) {
			}
			return;
		}
		
		
		if ( null == _login_time ) {
			String tmp_login_time = header.substring( 0, 19 );
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        try {
	        	formatter.parse(tmp_login_time);
	        	_login_time = tmp_login_time;
	        } catch (ParseException e) {
	        	return;
			}
		}
		
		// add it to all headers
		try_add_to_map( header );

		// really authenticated
		if( header.contains(SUCCESSFUL_AUTHENTICATE_HEADER) ) {
			_successful = true;
			try {
				Matcher matcher = _pattern.matcher(header);
				if ( matcher.find() ) {
					_protocol = matcher.group(1);
					_ip = matcher.group(2);
				}
			}catch(Exception e){
			}
			return;
		}

		
	} // add_header
}
