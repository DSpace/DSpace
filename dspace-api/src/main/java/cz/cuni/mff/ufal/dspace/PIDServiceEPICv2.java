/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.handle.HandleManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PIDServiceEPICv2 extends AbstractPIDService {

	Logger log = Logger.getLogger(PIDServiceEPICv2.class);
	
	public PIDServiceEPICv2() throws Exception {

	}

	@Override
	public String sendPIDCommand(HTTPMethod method, Map<String, Object> params) throws Exception {
		String PID = (String) params.get(PARAMS.PID.toString());
		String data = (String) params.get(PARAMS.DATA.toString());

		if (PID == null)
			PID = "";
		if (!PID.startsWith("/") && !PIDServiceURL.endsWith("/"))
			PID = "/" + PID;

		URL url = new URL(PIDServiceURL + PID);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method.toString());
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");
		
		Map<String, String> headers = (Map<String, String>)params.get(PARAMS.HEADER.toString());
		if(headers!=null)
		for(Entry<String, String> header : headers.entrySet()) {
			conn.setRequestProperty(header.getKey(), header.getValue());
		}

		if (data != null) {
			OutputStream out = conn.getOutputStream();
			out.write(data.getBytes());
			out.flush();
		}
		
		int responseCode = conn.getResponseCode();
		
		if (responseCode < 200 && responseCode > 206) {
			log.error("Failed : HTTP error code : " + responseCode + " : " + conn.getResponseMessage());
			throw new RuntimeException("Failed : HTTP error code : " + responseCode + " : " + conn.getResponseMessage());
		} else {
			log.debug(responseCode + " : " + conn.getResponseMessage());
		}

		StringBuffer response = new StringBuffer();			
		if(responseCode==201) {
			int index = PIDServiceURL.endsWith("/")?PIDServiceURL.length():(PIDServiceURL.length()+1);
			response.append(conn.getHeaderField("Location").substring(index));
		} else {
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String line = null;
			while ((line = br.readLine()) != null) {
				response.append(line).append("\n");
			}
		}
		conn.disconnect();
		return response.toString();
	}

	@Override
	public String resolvePID(String PID) throws Exception {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), PID);
		return sendPIDCommand(HTTPMethod.GET, params);
	}

	@Override
	public String createPID(Map<String, String> handleFields, String prefix) throws Exception {
		JsonArray data = getEPICJsonRepresentation(handleFields);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), prefix);
		params.put(PARAMS.DATA.toString(), data.toString());
		return sendPIDCommand(HTTPMethod.POST, params);
	}

	@Override
	public String createCustomPID(Map<String, String> handleFields, String prefix, String suffix) throws Exception {
		JsonArray data = getEPICJsonRepresentation(handleFields);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), HandleManager.completeHandle(prefix, suffix));
		params.put(PARAMS.DATA.toString(), data.toString());
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("If-None-Match", "*");

		params.put(PARAMS.HEADER.toString(), headers);

		return sendPIDCommand(HTTPMethod.PUT, params);
	}

	@Override
	public String modifyPID(String PID, Map<String, String> handleFields) throws Exception {
		JsonArray data = getEPICJsonRepresentation(handleFields);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), PID);
		params.put(PARAMS.DATA.toString(), data.toString());
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("If-Match", "*");
		
		params.put(PARAMS.HEADER.toString(), headers);

		return sendPIDCommand(HTTPMethod.PUT, params);
	}

	@Override
	public String deletePID(String PID) throws Exception {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), PID);
		return sendPIDCommand(HTTPMethod.DELETE, params);
	}

	@Override
	public String findHandle(Map<String, String> handleFields, String prefix) throws Exception {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAMS.PID.toString(), prefix + "/?" + getQueryString(handleFields));
		String response = sendPIDCommand(HTTPMethod.GET, params);
		String[] pids = new Gson().fromJson(response, String[].class);
	    if (pids.length == 0) {
	        return null;
	    }
	    return StringUtils.join(pids, ",");
	}

	@Override
    public boolean supportsCustomPIDs() {
        return true;
    }

	@Override
	public String whoAmI(String encoding) throws Exception {
		throw new NotImplementedException();
	}
	
	private String getQueryString(Map<String, String> handleFields) {
		StringBuffer qstr = new StringBuffer();
		for(Entry<String, String> entry : handleFields.entrySet()) {
			qstr.append("&");
			qstr.append(entry.getKey());
			qstr.append("=");
			qstr.append(entry.getValue());			
		}
		return qstr.substring(1);
	}
	
	private JsonArray getEPICJsonRepresentation(Map<String, String> handleFields) {
		JsonArray json_rep = new JsonArray();
		for(Entry<String, String> entry : handleFields.entrySet()) {
			JsonObject json_obj = new JsonObject();
			json_obj.addProperty("type", entry.getKey());
			json_obj.addProperty("parsed_data", entry.getValue());
			json_rep.add(json_obj);
		}
		return json_rep;
	}

}
