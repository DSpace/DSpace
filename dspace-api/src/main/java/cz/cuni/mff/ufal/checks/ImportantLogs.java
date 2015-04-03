/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.checks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;

import cz.cuni.mff.ufal.dspace.IOUtils;


//
//
//
public class ImportantLogs implements Iterable< String >  
{
	// ctors
	private static final String ERR = "ERROR";
	private static final String SEV = "SEVERE";
	private static final String CRIT = "CRIT";
	public final String today;

	// vars
	public ArrayList<String> _lines;
	ArrayList< String > _warnings;
	String _id;
	
	public static boolean is_bad_line( String line ) {
		return line.contains(ERR) || line.contains(SEV) || line.contains(CRIT);
	}
	
	public boolean is_exc_line( String line ) {
		return line.contains("Exception") || !line.startsWith(today);
	}

	public void add_to_lines( String line, LineNumberReader reader ) {
		String msg = reader.getLineNumber() + ":" + line;
		_lines.add( msg );
	}

	public void add_to_lines( String line, int num ) {
		String msg = String.valueOf(num) + ":" + line;
		_lines.add( msg );
	}
	
	public ImportantLogs( BufferedReader reader_raw, String date_string ) 
	{
		today = date_string != null ? date_string : IOUtils.today_string();
		_warnings = new ArrayList< String >();
		_lines = new ArrayList< String >();
		LineNumberReader reader = new LineNumberReader( reader_raw );
		/*LineNumberReader reader = new LineNumberReader( reader_raw ){
			@Override
			public String readLine() throws IOException{
				System.err.println(this.getLineNumber());
				return super.readLine();
			}
		};*/
		
		try {
			String line = reader.readLine();
			String last_line = null;
			do  
			{
				last_line = null;
					// anything more?
					if ( line == null ) {
						break;
					}
					// not real log line?
					if( !line.startsWith(today) ){
						continue;
					}
				
				// we are interested
				if( is_bad_line(line) )
				{
					String nextLine = reader.readLine();
						if ( nextLine == null ){
							add_to_lines( line, reader.getLineNumber() - 1 );
							last_line = nextLine;
							break;
						}
					
					// the next line can be an exc. too
					if ( is_bad_line(nextLine) && nextLine.startsWith(today) ) {
						add_to_lines( line, reader );
						last_line = nextLine;
					
					} else if ( is_exc_line(nextLine) ) {
						//add few lines if its exception
						String msg = line + "\n" + nextLine;
						nextLine = reader.readLine();
							if ( nextLine == null ){
								break;
							}
						msg += "\n" + nextLine;
						add_to_lines( msg, reader );
					}else {
						add_to_lines( line, reader.getLineNumber() - 1 );
					}
				}
				
			} while( null != (line= last_line != null ? last_line : reader.readLine()) );
		
		reader.close();
		
		} catch(Exception e) {
			_warnings.add( e.getMessage() );
			return;
		}
	
	}	

	public java.util.List<String> warnings() {
		return _warnings;
	}
	
	public Iterator< String > iterator() {        
        return _lines.iterator();
    }
	
	
} // ImportantLogs

