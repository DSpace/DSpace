/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.handle.service.HandleClarinService;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Service for PID EPICv2.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class PIDServiceEPICv2 extends AbstractPIDService {
    private static final Logger log = Logger.getLogger(PIDServiceEPICv2.class);
    private static final Type handleListType = new TypeToken<List<Handle>>() {}.getType();

    private HandleClarinService handleClarinService = ContentServiceFactory.getInstance().getHandleClarinService();

    public PIDServiceEPICv2() throws Exception {
        super();
    }

    @Override
    public String sendPIDCommand(HTTPMethod method, Map<String, Object> params)
            throws Exception {
        String PID = (String) params.get(PARAMS.PID.toString());
        String data = (String) params.get(PARAMS.DATA.toString());
        String prefix = null;

        if (Objects.isNull(PID)) {
            PID = "";
        } else {
            prefix = PID.startsWith("/") ? PID.split("/", 3)[1] : PID.split("/", 2)[0];
        }
        if (!PID.startsWith("/") && !PIDServiceURL.endsWith("/")) {
            PID = "/" + PID;
        }

        URL url = new URL(PIDServiceURL + PID);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method.toString());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        Map<String, String> headers = (Map<String, String>) params
                .get(PARAMS.HEADER.toString());
        if (Objects.nonNull(headers)) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (Objects.nonNull(data)) {
            OutputStream out = conn.getOutputStream();
            out.write(data.getBytes());
            out.flush();
        }

        int responseCode = conn.getResponseCode();

        if (responseCode < 200 && responseCode > 206) {
            log.error("Failed : HTTP error code : " + responseCode + " : "
                    + conn.getResponseMessage());
            throw new RuntimeException("Failed : HTTP error code : "
                    + responseCode + " : " + conn.getResponseMessage());
        } else {
            log.debug(responseCode + " : " + conn.getResponseMessage());
        }

        StringBuffer response = new StringBuffer();
        if (responseCode == 201) {
            String location = conn.getHeaderField("Location");
            int index;
            if (Objects.nonNull(prefix)) {
                index = location.indexOf(prefix);
            } else {
                index = PIDServiceURL.endsWith("/") ? PIDServiceURL.length()
                        : (PIDServiceURL.length() + 1);
            }
            response.append(location.substring(index));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String line = null;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
        }
        conn.disconnect();
        return response.toString();
    }

    /**
     * Returns URL
     */
    @Override
    public String resolvePID(String PID) throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(PARAMS.PID.toString(), PID);
        String response = sendPIDCommand(HTTPMethod.GET, params);
        Gson gson = getGsonWithHandleDeserializers(null);
        Handle handle = gson.fromJson(response, Handle.class);
        return handle.getUrl();
    }

    @Override
    public String createPID(Map<String, String> handleFields, String prefix)
            throws Exception {
        JsonArray data = getEPICJsonRepresentation(handleFields);
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(PARAMS.PID.toString(), prefix);
        params.put(PARAMS.DATA.toString(), data.toString());
        return sendPIDCommand(HTTPMethod.POST, params);
    }

    @Override
    public String createCustomPID(Map<String, String> handleFields,
                                  String prefix, String suffix) throws Exception {
        JsonArray data = getEPICJsonRepresentation(handleFields);
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(PARAMS.PID.toString(), handleClarinService.completeHandle(prefix, suffix));
        params.put(PARAMS.DATA.toString(), data.toString());

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("If-None-Match", "*");

        params.put(PARAMS.HEADER.toString(), headers);

        return sendPIDCommand(HTTPMethod.PUT, params);
    }

    @Override
    public String modifyPID(String PID, Map<String, String> handleFields)
            throws Exception {
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
    public String findHandle(Map<String, String> handleFields, String prefix)
            throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(PARAMS.PID.toString(), prefix + "/?"
                + getQueryString(handleFields));
        String response = sendPIDCommand(HTTPMethod.GET, params);
        String[] pids = new Gson().fromJson(response, String[].class);
        if (pids.length == 0) {
            return null;
        }
        return StringUtils.join(pids, ",");
    }

    public List<Handle> findHandles(Map<String, String> handleFields, String prefix,
                                    String depth, int limit, int page) throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        HashMap<String, String> headers = new HashMap<>();
        addDepth(headers, depth);
        if (!headers.isEmpty()) {
            params.put(PARAMS.HEADER.toString(), headers);
        }
        addLimitPage(handleFields, limit, page);
        params.put(PARAMS.PID.toString(), prefix + "/?"
                + getQueryString(handleFields));
        String response = sendPIDCommand(HTTPMethod.GET, params);
        Gson gson = getGsonWithHandleDeserializers(prefix);
        return gson.fromJson(response, handleListType);
    }

    public List<Handle> findHandles(String query, String prefix, String depth, int limit, int page) throws Exception {
        Map<String, String> handleFields = new HashMap<>();
        //surround the query with **, allows substring matches
        handleFields.put(HANDLE_FIELDS.URL.toString(), String.format("*%s*",query));
        return findHandles(handleFields, prefix, depth, limit, page);
    }

    @Override
    public boolean supportsCustomPIDs() {
        return true;
    }

    @Override
    public String whoAmI(String encoding) throws Exception {
        return "There is no implementation of whoAmI in v2 you are logging in as "
                + PIDServiceUSER;
    }

    public List<Handle> list(String prefix, String depth, int limit, int page)
            throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        HashMap<String, String> headers = new HashMap<>();
        addDepth(headers, depth);
        if (!headers.isEmpty()) {
            params.put(PARAMS.HEADER.toString(), headers);
        }

        HashMap<String, String> fields = new HashMap<>();
        addLimitPage(fields, limit, page);
        params.put(PARAMS.PID.toString(), prefix + "/?"
                + getQueryString(fields));
        String response = sendPIDCommand(HTTPMethod.GET, params);

        Gson gson = getGsonWithHandleDeserializers(prefix);
        return gson.fromJson(response, handleListType);
    }

    public List<Handle> listAllHandles(String prefix) throws Exception {
        return list(prefix, "1", 0, 0);
    }

    public int getCount(String prefix) throws Exception {
        return list(prefix,"0",0,0).size();
    }

    public int getResultCount(String prefix, String query) throws Exception {
        return findHandles(query, prefix, "0", 0, 0).size();
    }

    private String getQueryString(Map<String, String> handleFields) {
        StringBuffer qstr = new StringBuffer();
        for (Map.Entry<String, String> entry : handleFields.entrySet()) {
            qstr.append("&");
            qstr.append(entry.getKey());
            qstr.append("=");
            qstr.append(entry.getValue());
        }
        return qstr.substring(1);
    }

    private void addDepth(Map<String, String> headers, String depth) {
        if (depth != null && depth.matches("^(0|1|infinity)$")) {
            headers.put("Depth", depth);
        }
    }

    private void addLimitPage(Map<String, String> fields, int limit, int page) {
        fields.put("limit", Integer.toString(limit));
        if (limit > 0) {
            fields.put("page", Integer.toString(page));
        }
    }

    private Gson getGsonWithHandleDeserializers(String prefix) {
        // Configure Gson
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Handle.class, new HandleDeserializer());
        if (prefix != null) {
            gsonBuilder.registerTypeAdapter(handleListType, new HandlesDeserializer(
                    prefix));
        }
        return gsonBuilder.create();
    }

    private JsonArray getEPICJsonRepresentation(Map<String, String> handleFields) {
        JsonArray json_rep = new JsonArray();
        for (Map.Entry<String, String> entry : handleFields.entrySet()) {
            JsonObject json_obj = new JsonObject();
            json_obj.addProperty("type", entry.getKey());
            json_obj.addProperty("parsed_data", entry.getValue());
            json_rep.add(json_obj);
        }
        return json_rep;
    }

    public static class Handle {

        private String handle;

        private String url;

        public Handle(String handle, String url) {
            this.handle = handle;
            this.url = url;
        }

        public Handle(String handle) {
            this(handle, null);
        }

        public Handle() {
            this(null, null);
        }

        public String getHandle() {
            return handle;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setHandle(String handle) {
            this.handle = handle;
        }

    }

    static class HandleDeserializer implements JsonDeserializer<Handle> {

        @Override
        public Handle deserialize(JsonElement json, Type typeOfT,
                                  JsonDeserializationContext context) throws JsonParseException {
            JsonArray jsonInfo = json.getAsJsonArray();
            for (JsonElement el : jsonInfo) {
                JsonObject obj = el.getAsJsonObject();
                JsonElement jsonType = obj.get("type");
                if (jsonType != null) {
                    String type = jsonType.getAsString();
                    if (type.equals("URL")) {
                        String url = obj.get("parsed_data").getAsString();
                        Handle h = new Handle();
                        h.setUrl(url);
                        return h;
                    }
                }
            }
            throw new JsonParseException("Failed to find URL for this handle.\n" + json.toString());
        }
    }

    static class HandlesDeserializer implements JsonDeserializer<List<Handle>> {

        private final String prefix;

        public HandlesDeserializer(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public List<Handle> deserialize(JsonElement json, Type typeOfT,
                                        JsonDeserializationContext context) throws JsonParseException {
            ArrayList<Handle> handles = new ArrayList<>();
            if (json.isJsonArray()) {
                String[] ids = context.deserialize(json, String[].class);
                for (String id : ids) {
                    handles.add(new Handle(prefix + "/" + id));
                }
            } else {
                JsonObject jsonObject = json.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    // remove /handles/ to match ids provided with Depth: 0
                    String id = entry.getKey().replaceFirst("/handles/", "");
                    try {
                        Handle h = context.deserialize(entry.getValue(), Handle.class);
                        h.setHandle(id);
                        handles.add(h);
                    } catch (JsonParseException e) {
                        //there are handles with no url
                        Handle h  = new Handle();
                        h.setHandle(id);
                        handles.add(h);
                        //throw new JsonParseException("Failed to parse " + id, e);
                    }
                }
            }
            return handles;
        }
    }
}
