/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.checks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;

import com.ibm.icu.text.SimpleDateFormat;

import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.AbstractControlPanelTab;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper;


public class IsLoggingControlCheck extends  AbstractControlPanelTab {
	
	private Request request = null;
	protected HtmlHelper html = null;	

	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		div = div.addDivision( this.getClass().getSimpleName(), "control_check  well well-light" );

		request = ObjectModelHelper.getRequest(objectModel);
		html = new HtmlHelper( div, web_link );		
		
		HashSet<Logger> loggers = new HashSet<Logger>();
		HashSet<Object> appenders = new HashSet<Object>();
		//Get loggers and appenders (for filenames)
		for (Enumeration<?> e = LogManager.getCurrentLoggers(); e.hasMoreElements();){
			Logger log = (Logger)e.nextElement();
			for (Enumeration<?> en = log.getAllAppenders() ; en.hasMoreElements() ;) {
				Object o = en.nextElement();
				if(o != null && o.getClass().getName().equals(org.dspace.app.util.DailyFileAppender.class.getName())){
					loggers.add(log); //keep loggers just for FileAppenders				
					appenders.add(o);				
				}
				else{
					html.warning( "Skipping appender it is '" + o.getClass().getName() + "'" );
				}
			}
	    }
		
		Date date = new Date();
		String test = "TESTING LOGGER " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
		String regex = ".*" + test +".*";
		
		if(loggers.size()==0){
			html.exception( "Failed to find suitable loggers. Not checking!", null );
			return;
		}
		
		for(Logger log : loggers){
			//Log INFO with all loggers we've found
			log.info(test);
		}
		
		for(Object ap : appenders){
			//Now try to find the string in the log files
			String filename=null;
			try{
				
				String get_file_res = (String) ap.getClass().getMethod("getFile", null).invoke(ap, null);
				String get_date_pattern = (String)ap.getClass().getMethod("getDatePattern", null).invoke(ap,null);
				filename = get_file_res + "." + new SimpleDateFormat(get_date_pattern).format(date);
			}
			catch(Exception e){
				html.exception( e.toString(), null );
			}
			BufferedReader br = null;
			try{
				br = new BufferedReader(new FileReader(filename)); //Won't use safe_reader because of the empty check there
				String line;
				boolean found = false;
				while( null != (line=br.readLine()) ) {
					if(line.matches(regex)){
						found = true;
						break;
					}
				}
				
				if(found){
					html.ok( "Logging to " + filename + " OK." );
				}
				else{
					html.exception( "Logging to" + filename + " failed.", null );
					html.failed();
				}
				
			}catch(IOException e){
				html.exception( "Failed to read from file " + filename + "\n" + e.toString(), null );
				html.failed();
			}finally{
				if(br != null){
					try{
						br.close();
					}catch (IOException e){
						html.exception( "Failed to close file " + filename + "\n" + e.toString(), null );
					}
				}
			}
			
		}
	} // run
	
	
	// ===============
	//
	//
	static public void main( String[] args ){
	}   
	
} // IsLoggingControlCheck


