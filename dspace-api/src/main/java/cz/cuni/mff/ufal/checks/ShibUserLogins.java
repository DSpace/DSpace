/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.checks;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


public class ShibUserLogins implements Iterable<ShibUserLogin> 
{
	// constants
	private static final String START_KEY = "marshalled message";
	private static final String END_KEY_OK = "new session created:";
	private static final String END_KEY_FAIL = "marshalled message";
	
	// vars
	public ArrayList< ShibUserLogin > _logins;
	public ArrayList<String> _warnings;
	public int _logins_successful;
	
	// ctor - do all the stuff here
	public ShibUserLogins( BufferedReader reader ) 
	{
		_logins = new ArrayList< ShibUserLogin >();
		_warnings = new ArrayList< String >();
		_logins_successful = 0;
		
		boolean debug_met = false;
	
		try {
			String line = "";
			// case of exception
			String should_read_immediately = null;
			
			while( null != (line=reader.readLine()) ) 
			{
				if ( line.contains("DEBUG") ) {
					debug_met = true;
				}
				// go to the beginning of new block
				if( should_read_immediately == null && !line.contains(START_KEY) ) {
					continue;
				}
				
				// read until exception/end/whatever
				ShibUserLogin one_login = new ShibUserLogin(); 
				
				try {
					String header = should_read_immediately != null ? 
							line : reader.readLine();
					should_read_immediately = null;
					while( header != null ) 
					{
						
						// e.g., exception
						if( header.contains(START_KEY) ) {
							should_read_immediately = header;
							break;
						}

						// store it 
						one_login.add_header( header );
						
						// the end
						if( header.contains(END_KEY_FAIL) || header.contains(END_KEY_OK)  ) {
							break;
						}
						
						 header = reader.readLine();
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
		
		if ( !debug_met ) {
			_warnings.add( "Is DEBUG logging turned on?" );
		}
	
	}
	
	public java.util.List<String> warnings() {
		return _warnings;
	}
	
	public Iterator< ShibUserLogin > iterator() {        
  return _logins.iterator();
}


} // class UserLogins