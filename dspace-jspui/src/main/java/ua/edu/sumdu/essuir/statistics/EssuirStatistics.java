package ua.edu.sumdu.essuir.statistics;


import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.*;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import ua.edu.sumdu.essuir.utils.GeoIp;

import javax.servlet.http.HttpServletRequest;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class EssuirStatistics {

	public static Hashtable<Integer, Long> getViewStatistics(int[] ids) {
		return getStatistics(ids, -1);
	}

	
	public static Hashtable<Integer, Long> getDownloadStatistics(int[] ids) {
		return getStatistics(ids, 0);
	}
	
	
	private static void spy(int item_id, boolean bitstream, String ip) {
		if (item_id != 2822)
			return;
		
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream("D://spy_" + item_id + ".txt", true)));
			
			out.println((new java.util.Date()) + " - " + ip + " - " + (bitstream ? "download" : "view"));
			
			out.close();
		} catch (Exception e) {
			log.error("Spy error", e);
		}
	}
	
    public static String[][] updateItem(HttpServletRequest request, int item_id, double chance) {
//    	spy(item_id, false, request.getRemoteAddr());    	
    	
    	String countryCode = GeoIp.getCountryCode(request);

        if (countryCode != null)
	        update(request, item_id, -1, countryCode, chance);
        
        return select(request, item_id, -1, null);
    }

    
    public static String[][] updateBitstream(HttpServletRequest request, int item_id, int sequence_id) {
    	spy(item_id, true, request.getRemoteAddr());
        String countryCode = GeoIp.getCountryCode(request);
    	update(request, item_id, sequence_id, countryCode, 1);

    	String[][] views = select(request, item_id, sequence_id, countryCode);
    	
        if (views == null || views.length == 0) {
            String[][] tmp = new String[1][1];
            tmp[0][0] = "0";
            return tmp;
        }

    	return views;
    }
    
    
    public static String[][] selectItem(HttpServletRequest request, int item_id) {
        return select(request, item_id, -1, null);
    }

    
    public static String selectBitstream(HttpServletRequest request, int item_id, int sequence_id) {
    	String[][] views = select(request, item_id, sequence_id, null);
    	
        if (views == null || views.length == 0)
            return "0";

        int sumDownloads = 0;
        for (int i = 0; i < views.length; i++){
            sumDownloads += Integer.parseInt(views[i][1]);
        }

    	return Integer.valueOf(sumDownloads).toString();
    }

    public static String[][] selectBitstreamByCountries(HttpServletRequest request, int item_id, int sequence_id){
        String[][] views = select(request, item_id, sequence_id, null);

        if (views == null || views.length == 0)
            return null;

        return views;
    }

    private static void update(HttpServletRequest request, int item_id, int sequence_id, String countryCode, double chance) {
    	try {
	    	Context context = UIUtil.obtainContext(request);
	        
	    	String[][] views = null;
	
	        try {
	            views = select(request, item_id, sequence_id, countryCode);
	
	            if (views == null || views.length == 0) {
	                add(context, item_id, sequence_id, 1, countryCode);
	            } else {
	            	int cnts = Integer.parseInt(views[0][1]);
	            	Random rand = new Random();
	            	int a = rand.nextInt(1024);
	            	
	            	if (a < 1024 * chance)
	            		update(context, item_id, sequence_id, ++cnts, countryCode);
	            }
	        } catch (SQLException e) {
			context.getDBConnection().rollback();
	                log.error(e.getMessage(), e);
	        }
		} catch (SQLException ex) {
            		log.error(ex.getMessage(), ex);
		}
    }
    
    
    private static String[][] select(HttpServletRequest request, 
    		int item_id, int sequence_id, String countryCode) {
    	try {
	    	String query = "SELECT * " +
       				"FROM statistics " +
       				"WHERE item_id=" + item_id;

            if (sequence_id != 0) {
                query += " AND sequence_id = " + sequence_id;
            }
            /* This code is used for getting information about amount of downloads*/
            else{
                query += " AND sequence_id > " + sequence_id;
            }

	        if (countryCode != null)
	        	query += " AND country_code='" + countryCode + "'";

	        ArrayList<String[]> res = new ArrayList<String[]>();
	        Connection c = null;
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
        	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                	                            ConfigurationManager.getProperty("db.username"),
                        	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

        	    ResultSet resSet = s.executeQuery(query);

	            while (resSet.next()) {
		            String[] data = new String[2];
		            
	        	    data[0] = resSet.getString("country_code");
		            data[1] = resSet.getInt("view_cnt") + "";
	            
		            res.add(data);
        	    }
	
        	    s.close();
	        } finally {
	            if (c != null) 
	                c.close();
	        }
	
	        String[][] views = new String[res.size()][2];
	        for (int i = 0; i < res.size(); i++) 
	        	views[i] = res.get(i);
	        
	        return views;
	} catch (Exception ex) {
        	log.error(ex.getMessage(), ex);
		return null;
	}
    }

    
    private static boolean update(Context context, int item_id, int sequence_id, int views, String countryCode) throws SQLException {
        String query = "UPDATE statistics " +
			"SET view_cnt=" + views + 
			" WHERE item_id=" + item_id + 
			" AND sequence_id=" + sequence_id;

	query += " AND country_code='" + countryCode + "'";

        Connection c = null;
	int res = 0;
	try {
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
	       	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
	               	                            ConfigurationManager.getProperty("db.username"),
	                       	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

       		    res = s.executeUpdate(query);

	       	    s.close();
	        } finally {
	            if (c != null) 
        	        c.close();
	        }
	} catch (Exception ex) {
		log.error(ex.getMessage(), ex);
	}

	if (res == 1) {
            return true;
        } else {
            return false;
        }
    }

    
    private static boolean add(Context context, int item_id, int sequence_id, int views, String countryCode) throws SQLException {
	String query = "INSERT INTO statistics " + 
				"VALUES (" + item_id + ", " + sequence_id + ", " + views  + ", '" + countryCode + "')";

        Connection c = null;
	int res = 0;
	try {
	        try {
        	    Class.forName(ConfigurationManager.getProperty("db.driver"));
        
	       	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
        	       	                            ConfigurationManager.getProperty("db.username"),
                	       	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

       		    res = s.executeUpdate(query);

	       	    s.close();
	        } finally {
	            if (c != null) 
	                c.close();
	        }
	} catch (Exception ex) {
		log.error(ex.getMessage(), ex);
	}

        if (res == 1) {
            return true;
        } else {
            return false;
        }
    }

    
    public static StatisticData getTotalStatistic(Context context) {
    	StatisticData sd = new StatisticData();
    	
    	try {
	        Connection c = null;
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
        	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                	                            ConfigurationManager.getProperty("db.username"),
                        	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

		    	String query = "SELECT COUNT(*) submits FROM item WHERE in_archive; ";
	            
	            ResultSet resSet = s.executeQuery(query);

	            while (resSet.next()) {
		            sd.setTotalCount(resSet.getLong("submits"));
        	    }
	
        	    s.close();
        	    
        	    sd.setLastUpdate(getLastUpdate(context));
        	    sd.setTotalViews(getStatistics(-1));
        	    sd.setTotalDownloads(getStatistics(0));
	        } finally {
	            if (c != null) 
	                c.close();
	        }
		} catch (Exception ex) {
        	log.error(ex.getMessage(), ex);
		}
    	
    	return sd;
    }
    
    
    private static String getLastUpdate(Context context) {
        java.util.Date dateNow = new java.util.Date();
        
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
 
        String lastUpdate = df.format(dateNow);    	

    	try {
    		// get our configuration
    		String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
    		String count = "1";
    		
    		// prep our engine and scope
    		BrowseEngine be = new BrowseEngine(context);
    		BrowserScope bs = new BrowserScope(context);
    		BrowseIndex bi = BrowseIndex.getItemBrowseIndex();
    		
    		// fill in the scope with the relevant gubbins
    		bs.setBrowseIndex(bi);
    		bs.setOrder(SortOption.DESCENDING);
    		bs.setResultsPerPage(Integer.parseInt(count));
    	    for (SortOption so : SortOption.getSortOptions()) {
    	        if (so.getName().equals(source))
    	            bs.setSortBy(so.getNumber());
    	    }
    		
    		BrowseInfo results = be.browseMini(bs);
    		
    		Item[] items = results.getItemResults(context);
    		
    		Metadatum[] dcv = items[0].getMetadata("dc", "date", "accessioned", Item.ANY);
			if (dcv != null) {
				if (dcv.length > 0) {
					String date = dcv[0].value;
					
					String year = date.substring(0, 4);
					String month = date.substring(5, 7);
					String day = date.substring(8, 10);
					
					lastUpdate = day + "." + month + "." + year;
				}
			}
    	} catch (SortException se) {
    	    se.printStackTrace();
    	} catch (BrowseException e) {
    		e.printStackTrace();
    	}
    	
    	return lastUpdate;
    }
    
    
    private static long getStatistics(int sequence_id) {
    	long sum = -1;
    	
    	try {
	    	String query = "SELECT SUM(view_cnt) AS sum " +
       				"FROM statistics " +
       				"WHERE sequence_id " + (sequence_id >= 0 ? ">=" : "<") + " 0 ";
	        
	        Connection c = null;
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
        	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                	                            ConfigurationManager.getProperty("db.username"),
                        	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

        	    ResultSet resSet = s.executeQuery(query);

	            while (resSet.next()) {
		            sum = resSet.getLong("sum");
        	    }
	
        	    s.close();
	        } finally {
	            if (c != null) 
	                c.close();
	        }
		} catch (Exception ex) {
        	log.error(ex.getMessage(), ex);
		}
    	
        return sum;
    }
    
    
    private static Hashtable<Integer, Long> getStatistics(int[] item_ids, int sequence_id) {
    	Hashtable<Integer, Long> table = new Hashtable<Integer, Long>();
    	
    	try {
    		StringBuilder sb = new StringBuilder();
    		if (item_ids.length > 0) {
    			table.put(item_ids[0], 0L);
    			sb.append(item_ids[0]);
    		}
    		for (int i = 1; i < item_ids.length; i++) {
    			table.put(item_ids[i], 0L);
    			sb.append(", " + item_ids[i]);
    		}
    		
	    	String query = "SELECT item_id, SUM(view_cnt) AS sum " +
       				"FROM statistics " +
       				"WHERE item_id IN (" + sb.toString() + ") AND sequence_id " + (sequence_id >= 0 ? ">=" : "<") + " 0 " +
       				"GROUP BY item_id";
	        
	        Connection c = null;
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
        	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                	                            ConfigurationManager.getProperty("db.username"),
                        	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

        	    ResultSet resSet = s.executeQuery(query);

	            while (resSet.next()) {
		            table.put(resSet.getInt("item_id"), resSet.getLong("sum"));
        	    }
	
        	    s.close();
	        } finally {
	            if (c != null) 
	                c.close();
	        }
		} catch (Exception ex) {
        	log.error(ex.getMessage(), ex);
		}
    	
        return table;
    }
    
    
    
	private static Logger log = Logger.getLogger(EssuirStatistics.class);
}
