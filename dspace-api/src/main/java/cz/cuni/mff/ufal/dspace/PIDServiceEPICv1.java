/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;


public class PIDServiceEPICv1 extends AbstractPIDService {

	Logger log = Logger.getLogger(PIDServiceEPICv1.class);

	
	public PIDServiceEPICv1() throws Exception {

	}

	private URLConnection get_connection(HTTPMethod method, String command, String data) throws Exception {
		URL url = null;
		if (method == HTTPMethod.GET) {
			if (data != null && data.length() > 0) {
				data = "?" + data;
			}
			String url_str = PIDServiceURL + command + data;
			url = new URL(url_str);
			return url.openConnection();
		} else {
			String url_str = PIDServiceURL + command;
			url = new URL(url_str);
			// <OLD>
			HttpURLConnection httpconn = (HttpURLConnection) url
					.openConnection();
			httpconn.setRequestMethod("POST");
			httpconn.setDoOutput(true);
			OutputStream out = httpconn.getOutputStream();
			OutputStreamWriter wr = new OutputStreamWriter(out);
			// </OLD>

			// <NEW>
			// Security.addProvider(new
			// com.sun.net.ssl.internal.ssl.Provider());
			// SSLSocketFactory factory =
			// (SSLSocketFactory)SSLSocketFactory.getDefault() ;
			// SSLSocket socket = (SSLSocket)factory.createSocket(url.getHost(),
			// 443);
			// OutputStreamWriter wr = new OutputStreamWriter (
			// socket.getOutputStream() ) ;
			// </NEW>

			wr.write(data);
			wr.flush();
			return httpconn;
		}
	}

	@Override
	public String sendPIDCommand(HTTPMethod method, Map<String, Object> params) throws Exception {
		StringBuilder raw_response = new StringBuilder();
		String exc = "<no exception>";

		String command = (String) params.get(PARAMS.COMMAND.toString());
		String data = (String) params.get(PARAMS.DATA.toString());
		String match_regex = (String) params.get(PARAMS.REGEX.toString());

		try {
			URLConnection conn = get_connection(method, command, data);
			// APIv1
			InputStream input = conn.getInputStream();
			// ==

			// <NEW>
			// input = socket.getInputStream();
			// </NEW>

			String response = get_response(input, raw_response, match_regex);
			// the only correct response
			// otherwise generate exception
			if (null != response) {
				return response;
			}

		} catch (Exception e) {
			exc = e.toString() + "\n" + ExceptionUtils.getStackTrace(e);
		}
		String err = String.format(
				"Invalid PID service response:\n[%s]\n\terror:[%s]",
				raw_response.toString(), exc);
		throw new Exception(err);
	}

	private static String get_response(InputStream input, StringBuilder sb,
			String match_regex) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(input));
		String response = "";
		String line;
		do {
			line = in.readLine();
			// log.
			if (line == null)
				break;
			else
				response += line + "\n";
		} while (response.length() < 5 * 1024); // reasonable threshold of 5KB
		sb.append(response);

		if (match_regex != null) {
			Matcher m = Pattern.compile(match_regex).matcher(response);
			if (m.find()) {
				return m.group(1);
			}
		} else {
			return response;
		}
		return null;
	}

	@Override
	public String resolvePID(String PID) throws Exception {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.COMMAND.toString(), "read/view");
		params.put(PARAMS.DATA.toString(), "showmenu=no" + "&pid=" + URLEncoder.encode(PID, "UTF-8"));
		params.put(PARAMS.REGEX.toString(), "<tr><td>Location</td><td>([^<]+)</td>");		
		return sendPIDCommand(HTTPMethod.GET, params);
	}

	@Override
	public String createPID(Map<String, String> handleFields, String prefix) throws Exception {
		String URL = handleFields.get(HANDLE_FIELDS.URL.toString());
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.COMMAND.toString(), "write/create");
		params.put(PARAMS.DATA.toString(), "url=" + URLEncoder.encode(URL, "UTF-8"));
		params.put(PARAMS.REGEX.toString(), "<h2><a href=\"[^\"]*\">([^<]+)</a>");				
		return sendPIDCommand(HTTPMethod.POST, params);
	}

	@Override
	public String createCustomPID(Map<String, String> handleFields, String prefix, String suffix) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String modifyPID(String PID, Map<String, String> handleFields) throws Exception {
		String URL = handleFields.get(HANDLE_FIELDS.URL.toString());
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.COMMAND.toString(), "write/modify");
		params.put(PARAMS.DATA.toString(), "pid=" + URLEncoder.encode(PID, "UTF-8") + "&url=" + URLEncoder.encode(URL, "UTF-8"));
		params.put(PARAMS.REGEX.toString(), "<tr><td>Location</td><td>([^<]+)</td>");				
		return sendPIDCommand(HTTPMethod.POST, params);
	}

	@Override
	public String deletePID(String PID) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String findHandle(Map<String, String> handleFields, String prefix) throws Exception {
		String URL = handleFields.get(HANDLE_FIELDS.URL.toString());
		String resp = null;
		try{
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(PARAMS.COMMAND.toString(), "read/search");
			params.put(PARAMS.DATA.toString(), "url=" + URLEncoder.encode(URL, "UTF-8"));
			params.put(PARAMS.REGEX.toString(), "<tr><td>Handle</td><td>([^<]+)</td>");							
			resp = sendPIDCommand(HTTPMethod.GET, params);
		}catch(Exception e){
			if(e.getMessage().contains("FileNotFoundException")){
				return null;
			}else{
				throw e;
			}
			
		}
		return resp;
	}

	@Override
	public boolean supportsCustomPIDs() {
	    return false;
	}

	@Override
	public String whoAmI(String encoding) throws Exception {
		if(encoding == null) {
			encoding = "";
		}else {
			encoding = "?" + encoding;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.COMMAND.toString(), "write/whoami" + encoding);
		params.put(PARAMS.DATA.toString(), "");
		return sendPIDCommand(HTTPMethod.GET, params);
	}

}
