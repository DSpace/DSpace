/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import org.dspace.app.statistics.LogAnalyser;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;

/**
 * 
 */
public class ControlPanelStatisticsTab extends AbstractControlPanelTab {

    private String get_param(String param, String default_value) {
        return get_param( param, default_value, null );
    }

    private String get_param(String param, String default_value, String module) {
        String value = null;
        if ( null == module ) {
            value = ConfigurationManager.getProperty(param);
        }else {
            value = ConfigurationManager.getProperty(module, param);
        }
        if (value == null || value.length() == 0)
            value = default_value;
        return value;

    }
	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {
		
		List info = div.addList("statistics-info-ga");
		info.setHead("Configuration for Google Analytics");
		info.addLabel("GA");
		info.addItem(get_param("xmlui.google.analytics.key", "not defined"));
		
		info = div.addList("statistics-info");
		info.setHead("Configuration for default statistics");
		
		info.addLabel("Exclude bots (spiders)");
		info.addItem(get_param("query.filter.isBot", "default (true)"));
		
		info.addLabel("Spider urls");
		info.addItem(get_param("spiderips.urls", "not defined!", "solr-statistics"));
		
		try {
			LogAnalyser.readConfig();
			for (String[] ss : new String[][] { 
					new String[] { "excludeWords", "Excluded words"},
					new String[] { "itemLookup", "Lookup the names of first N items"},
					new String[] { "itemTypes", "Divide types"},
                    new String[] { "itemFloor", "Showing item viewed more than"},
                    new String[] { "searchFloor", "Showing searches hit more than"},
                    new String[] { "actionFloor", "Showing actions done more than"},
                    new String[] { "actionIgnores", "Ignoring action log lines containing"},
					}) 
			{
				java.lang.reflect.Field f = LogAnalyser.class.getDeclaredField(ss[0]);
				f.setAccessible(true);
				info.addLabel(ss[1]);
				info.addItem(f.get(null).toString());
			}
			
			// We need more so do it manually
			// from CreateStatReport
	        FileInputStream fis = new java.io.FileInputStream(new File(
	            ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + "dstat.cfg"));
	        Properties config = new Properties();
	        config.load(fis);
            info.addLabel("Start month/year");
            info.addItem(String.format("%s/%s", 
                            config.getProperty("start.month", "undefined"),
                            config.getProperty("start.year", "undefined")
                            ));
	        
		} catch (Exception e) {
			info.addLabel("Exception occurred!");
			info.addItem(e.toString());
		}
		
	}

}


