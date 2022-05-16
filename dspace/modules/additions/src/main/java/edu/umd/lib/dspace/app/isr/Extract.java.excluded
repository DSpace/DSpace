package edu.umd.lib.dspace.app.isr;

import java.util.HashMap;

import java.io.FileOutputStream;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.helpers.AttributesImpl;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Extract ISR tech reports from the rdbms outputting to xml.
 */

public class Extract {

  private static Logger log = Logger.getLogger(Extract.class);

  public static Connection conn = null;

  private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

  public static void main(String[] args) throws Exception {

    // dspace properties
    String strDspace     = configurationService.getProperty("dspace.dir");

    // Log4j configuration
    PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");

    // jdbc connection
    log.info("Setting up JDBC connection");
    Class driverClass = Class.forName(configurationService.getProperty("db.driver"));
    Driver driver = (Driver) driverClass.newInstance();
    DriverManager.registerDriver(driver);

    conn = DriverManager.getConnection(configurationService.getProperty("isr.url"), "dspace", null);

    // Setup the output xml
    FileOutputStream fos = new FileOutputStream(args[0]);
    OutputFormat format = OutputFormat.createPrettyPrint();
    XMLWriter out = new XMLWriter(fos, format);

    out.startDocument();
    out.startElement("","","collection",new AttributesImpl());
 
    Statement st = conn.createStatement();
    String strQuery = 
      "SELECT title,author,year,submitteruid,isrnum,published,papertype,advisoruid,advisors,filename,keywords,center,abstract,intelprop"
      + " FROM isr_report"
      ;
    
    ResultSet rs = st.executeQuery(strQuery);
    ResultSetMetaData rsm = rs.getMetaData();

    while (rs.next()) {
      out.startElement("","","record",new AttributesImpl());

      String strCenterKey = "";

      for (int i=1; i <= rsm.getColumnCount(); i++) {
	String strName = rsm.getColumnName(i);
	String strValue = rs.getString(i);

	AttributesImpl attr = new AttributesImpl();

	// add user info
	if ((strName.equals("submitteruid") || 
	     (strName.equals("advisoruid")))
	    && strValue != null) {
	  addUser(strValue, attr);
	}

	// build the center key
	if (strName.equals("year") ||
	    strName.equals("isrnum") ||
	    strName.equals("papertype"))
	{
	  strCenterKey += strValue;
	}

	// add center number
	if (strName.equals("center") && !strValue.equalsIgnoreCase("ISR")) {
	  strCenterKey = strValue.toLowerCase() + strCenterKey;
	  addCenterNumber(strCenterKey, attr);
	}

	// fixups
	if (strValue == null) {
	  strValue = "null";
	}
	if (strName.equals("year")) {
	  strValue = strValue.substring(0,4);
	}
	if (strName.equals("center")) {
	  strValue = strValue.toUpperCase();
	}

	// strip out control characters
	StringBuffer sb = new StringBuffer(strValue);
	for (int j=0; j < sb.length(); j++) {
	  if (Character.isISOControl(sb.charAt(j))) {
	    sb.setCharAt(j, ' ');
	  }
	}
	strValue = sb.toString();

	// output the field
	out.startElement("","",strName,attr);
	out.characters(strValue.toCharArray(), 0, strValue.length());
	out.endElement("","",strName);
      }

      out.endElement("","","record");
    }

    rs.close();
    st.close();

    // Close the xml output
    out.endElement("","","collection");
    out.endDocument();
    out.close();
  }


  /**
   * Add user information to the attributes.
   */

  private static HashMap hUser = null;

  public static void addUser(String strUserid, AttributesImpl attr) throws Exception {
    if (hUser == null) {
      log.info("Getting user information");
      hUser = new HashMap();

      Statement st = conn.createStatement();
      String strQuery = 
        "SELECT *"
        + " FROM users"
        ;
            
            ResultSet rs = st.executeQuery(strQuery);

            while (rs.next()) {
        String strUseridd = rs.getString("userid");
        String strFname = rs.getString("fname");
        String strLname = rs.getString("lname");
        String strUserType = rs.getString("usertype");

        hUser.put(strUseridd, new String[] {strFname, strLname, strUserType});
      }

      rs.close();
      st.close();
    }


    if (!hUser.containsKey(strUserid)) {
      throw new Exception("Unknown userid: " + strUserid);
    }

    String s[] = (String [])hUser.get(strUserid);

    attr.addAttribute("","","fname","",s[0]);
    attr.addAttribute("","","lname","",s[1]);
    attr.addAttribute("","","usertype","",s[2]);
  }


  /**
   * Add the center specific number
   */

  private static HashMap hCenter = null;

  public static void addCenterNumber(String strCenterKey, AttributesImpl attr) throws Exception {
    if (hCenter == null) {
      log.info("Getting center information");
      hCenter = new HashMap();

      String sCenter[] = new String[] {"caar","cdcss","cshcn","nextor","seil"};
      for (int i = 0; i < sCenter.length; i++) {

        Statement st = conn.createStatement();
        String strQuery = 
          "SELECT year,isrnum,papertype,centernum"
          + " FROM " + sCenter[i]
          ;

        ResultSet rs = st.executeQuery(strQuery);

        while (rs.next()) {
          String strKey = 
            sCenter[i]
            + rs.getString(1)
            + rs.getString(2)
            + rs.getString(3)
            ;

          hCenter.put(strKey, rs.getString(4));
        }

        rs.close();
        st.close();
      }
    }

    
    if (!hCenter.containsKey(strCenterKey)) {
      throw new Exception("Unknown center key: " + strCenterKey);
    }
    
    String sCenterNum = (String)hCenter.get(strCenterKey);
    
    attr.addAttribute("","","centernum","",sCenterNum);
  }


}





