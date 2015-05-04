/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;
import java.util.Scanner;

import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;

public class Info {
	
	
	final static public String get_proc_uptime()
	{
        // ufal
        String uptime = "unknown";
        try {
            uptime = new Scanner(new FileInputStream("/proc/uptime")).next();
			float fuptime = Float.parseFloat( uptime );
			int seconds = (int) (fuptime % 60);
			int minutes = (int) ((fuptime / 60) % 60);
			int hours   = (int) ((fuptime / (60*60)) % 24);
			int days   = (int) ((fuptime / (60*60*24)) );
			return Integer.toString( days ) + "d " +
				Integer.toString( hours ) + "h:" +
				Integer.toString( minutes ) + "m." +
				Integer.toString( seconds );
        }catch(Exception e){
			return uptime;
        }
	}
	
	final static public String get_jvm_uptime()
	{
    	RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
		long milliseconds = mxBean.getUptime();
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000*60)) % 60);
		int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
		int days   = (int) ((milliseconds / (1000*60*60*24)));

		return Integer.toString( days ) + "d " + 
			       Integer.toString( hours ) + "h:" + 
						 Integer.toString( minutes ) + "m." + 
						 Integer.toString( seconds );		
	}

	final static public String get_jvm_startime()
	{
    	RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
		return new Date( mxBean.getStartTime() ).toString(); 
	}
	
	final static public String get_jvm_version()
	{
    	RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    	return mxBean.getVmVersion();
	}
	
	final static public String get_ufal_build_time()
	{
    	final String buildTime = ConfigurationManager.getProperty("lr", "ufal.build_time");
    	if(buildTime != null && !buildTime.equals("")){
    		return buildTime;
    	}
		return "unknown";
	}

	final static public String get_global_connections_count() {
		return String.valueOf(DSpaceApi.getFunctionalityManager().getGlobalConnectionsCount());
	}

	final static public String get_session_open_count() {
		return String.valueOf(DSpaceApi.getFunctionalityManager().getSessionOpenCount());
	}

	final static public String get_session_close_count() {
		return String.valueOf(DSpaceApi.getFunctionalityManager().getSessionCloseCount());
	}

}
