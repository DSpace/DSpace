/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ibm.icu.text.SimpleDateFormat;

public class IOUtils {

	private static final Pattern date_pattern = Pattern.compile("(\\d\\d\\d\\d-\\d\\d-\\d\\d)");
	private static Logger log = Logger.getLogger(IOUtils.class);

	// ======================================
	// helpers
	//
	
	/**
	 * 
	 * @return
	 */
	static private String date_suffix( String format ) {
		return new SimpleDateFormat(
			format == null ? "yyyy-MM-dd" : format ).format(new Date());
	}
	
	/**
	 * 
	 * @param log_file_base
	 * @return
	 */
	static public String log_with_date( String log_file_base, String format ) {
		if ( log_file_base.endsWith(".") )
			return log_file_base + date_suffix(format);
		else
			return log_file_base + "." + date_suffix(format);
	}

	
	static public String[] list_files( final String dir_name, final String must_contain )
	{
		File dir = new File( dir_name );
		File[] tmp = dir.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return -1 != name.indexOf( must_contain );
		    }
		});	
		if ( null == tmp )
			return null;
		String[] files = new String[tmp.length];
		for ( int i = 0; i < tmp.length; ++i ) {
			files[i] = tmp[i].getName();
		}
		return files;
	}
	
	static public String get_date_from_log_file( String log_filename ) {
		Matcher matcher = date_pattern.matcher(log_filename);
		if ( matcher.find() ) {
			return matcher.group(1);
		}
		return null;
	}
	
	static public String[] list_dates()
	{
		String end_date_str = today_string();
		String start_date_str = week_before(new Date());
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date start_date;
		Date end_date;
		try {
			start_date = (Date)formatter.parse(start_date_str);
			end_date = (Date)formatter.parse(end_date_str); 
		} catch (ParseException e) {
			return null;
		} 
		
		List<String> dates = new ArrayList<String>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime( start_date );

	     while (calendar.getTime().before(end_date))
	     {
	         Date resultado = calendar.getTime();
	         dates.add(new SimpleDateFormat("yyyy-MM-dd").format(resultado));
	         calendar.add(Calendar.DATE, 1);
	     }
	     return dates.toArray(new String[dates.size()]);
	}
	

	/**
	 * 
	 * @param input_file
	 * @return
	 * @throws InstantiationException
	 */
	static public BufferedReader safe_reader( String input_file ) 
			throws EOFException, InstantiationException 
    {
		
		File file = new File(input_file);
		String file_id = file.getPath();
		
		if( !file.exists() ) {
			throw new InstantiationException( file_id + " does not exist!" );
		}
		if( file.exists() && 0 == file.length() ) {
			throw new EOFException( file_id + " is empty!" );
		}
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), Charset.forName("UTF8")) );
		} catch( IOException e ) {
			throw new InstantiationException( file_id + " exception while reading: " + e.toString() ); 
		}
		
		return reader;
	}
	
	public static long dir_size(File dir) {
	    long size = 0;
      if ( dir == null )
        return -1;
      File[] files = dir.listFiles();
      if ( files == null )
        return -2;
	    for (File file : files) {
	        if (file.isFile()) {
	            size += file.length();
	        }else {
	            size += dir_size(file);
	        }
	    }
	    return size;
	}	

	static public String today_string() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}
	
	static public String week_before(Date ref) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime( ref );
		calendar.add(Calendar.DAY_OF_MONTH, -7);
		return new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
	}
	
	public static String run( File where, String[] cmd) {
		String message = null;
      try {
          ProcessBuilder pb = new ProcessBuilder(cmd);
          pb.directory( where );
          Process p = pb.start();
          BufferedReader std_out = new BufferedReader(new 
              InputStreamReader(p.getInputStream()) );
          BufferedReader std_err = new BufferedReader(new 
              InputStreamReader(p.getErrorStream()) );

          String s = null;
          message = "Returned stdout:\n";
          while ((s = std_out.readLine()) != null) {
            message += s + "\n  ";
              }       
          message += "Returned stderr:\n";
          while ((s = std_err.readLine()) != null) {
            message += s + "\n  ";
              }       

          
           //Wait to get exit value
          int exitValue = p.waitFor();
          message += "Exit code: [" + String.valueOf( exitValue ) + "]\n";

        } catch (Exception e) {
          message += "\nException:" + e.toString();
          log.error( e );
        }
        return message;
	}	
	
}

