/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;

public class ControlPanelShibbolethTab extends AbstractControlPanelTab {
	
	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelShibbolethTab.class);
		
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
		
		addResult(div);

        // Links
		final String protocol = "https://";
		final String host = ConfigurationManager.getProperty("dspace.hostname");
		final String s_session = protocol + host + "/Shibboleth.sso/Session";
		final String s_metadata = protocol + host + "/Shibboleth.sso/Metadata";
		final String clarin_test = protocol + host + "/secure/shib_test.pl";
		final String s_login = protocol + host + 
				ConfigurationManager.getProperty("authentication-shibboleth", "lazysession.loginurl");
		
		List l = div.addList("control_panel-shibboleth");
		l.addLabel("Clarin Shibboleth test");
		l.addItemXref(clarin_test, clarin_test);

		l.addLabel("Shibboleth session");
		l.addItemXref(s_session, s_session);
		l.addLabel("Shibboleth metadata");
		l.addItemXref(s_metadata, s_metadata);

		l.addLabel("Shibboleth login url");
		l.addItemXref(s_login, s_login);

        	final String edugain_mon = "http://monitor.edugain.org/coc/?show=list_sp_tests";
        	l.addLabel("eduGAIN shibboleth monitoring url: ");
        	l.addItemXref(edugain_mon, edugain_mon);
		
		//
		boolean autoreg = ConfigurationManager.getBooleanProperty("authentication-shibboleth","autoregister", true);
		l.addLabel("Autoregister");
		l.addItem(String.valueOf(autoreg));
		
		l.addLabel("Default roles");
		l.addItem(String.valueOf(
				ConfigurationManager.getProperty("authentication-shibboleth","default-roles")));
		l.addLabel("Role header");
		l.addItem(ConfigurationManager.getProperty("authentication-shibboleth","role-header"));
		
		l.addLabel("Ignore scope");
		l.addItem(String.valueOf(ConfigurationManager.getBooleanProperty("authentication-shibboleth","role-header.ignore-scope", true)));
		l.addLabel("Ignore value");
		l.addItem(String.valueOf(ConfigurationManager.getBooleanProperty("authentication-shibboleth","role-header.ignore-value", false)));
		l.addLabel("Default group");
		l.addItem(ConfigurationManager.getProperty("authentication-shibboleth", "default.auth.group"));

		l = div.addList("shib-organistion");
		l.setHead("Organisations mapping");
		l.addItem("info","bold").addContent("The mapping is no longer used");

		l = div.addList("shib-status");
		l.addLabel(null, "bold").addContent("Shibboleth status");
		final String shib_status = "https://127.0.0.1/Shibboleth.sso/Status";
		l.addItemXref(shib_status, shib_status);
        
        l = div.addList("shib-attributes");
        l.setHead("Shibboleth attributes history/listing (production only)");
        final String shib_atts = "https://lindat.mff.cuni.cz/secure/attributes.xml";
        l.addItemXref(shib_atts, shib_atts);
        
        l = div.addList("shib-privacy");
        l.setHead("Privacy because of CoC (production only)");
        
	final String shib_privacy = "http://lindat.mff.cuni.cz/privacypolicy.html";
        l.addItemXref(shib_privacy, shib_privacy);
		
		
		l.addLabel();
		Item i = l.addItem();
		Button b = i.addButton("shibboleth-status");
		b.setValue("Status from server");

	}
	
	private void addResult(Division div) throws WingException {
		final String shib_status = "https://127.0.0.1/Shibboleth.sso/Status";
		String ret = "";
        if ( request.getParameter("shibboleth-status") != null) 
        {
			try {
				URL url = new URL(shib_status);
    			/*
    			 *  source: http://www.rgagnon.com/javadetails/java-fix-certificate-problem-in-HTTPS.html
    		     *  fix for
    		     *    Exception in thread "main" javax.net.ssl.SSLHandshakeException:
    		     *       sun.security.validator.ValidatorException:
    		     *           PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
    		     *               unable to find valid certification path to requested target
    		     */
    			try {
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, new TrustManager[] {
						       new X509TrustManager() {
						           public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						             return null;
						           }

						           public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

						           public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

						        }
						     }, new SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
					 // Create all-trusting host name verifier
				    HostnameVerifier allHostsValid = new HostnameVerifier() {
				        public boolean verify(String hostname, SSLSession session) {
				          return true;
				        }
				    };
				    // Install the all-trusting host verifier
				    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
					
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (KeyManagementException e) {
					e.printStackTrace();
				}				
	    		BufferedReader in = new BufferedReader(
	    		        new InputStreamReader(url.openStream()));
	
		        String inputLine;
		        while ((inputLine = in.readLine()) != null)
		            ret += inputLine;
		        in.close();	
			} catch (IOException e) {
				ret += String.format(
						"Exception while reading %s: [%s]", 
						shib_status, e.toString());
			} 
        }
	    
        div.addDivision("shibboleth-result", "result").addPara(ret);
	}

}

