/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import static org.dspace.iiif.canvasdimension.Util.checkDimensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.content.Bitstream;
import org.dspace.iiif.util.IIIFSharedUtils;


/**
 * Queries the configured IIIF image server via the Image API.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class IIIFApiQueryServiceImpl implements IIIFApiQueryService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFApiQueryServiceImpl.class);

    @Override
    public int[] getImageDimensions(Bitstream bitstream) {
        int[] arr = new int[2];
        String path = IIIFSharedUtils.getInfoJsonPath(bitstream);
        BufferedReader in = null;
        try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build()) {
            CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(path));
            in = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            JsonNode parent = new ObjectMapper().readTree(response.toString());
            // return dimensions if found.
            if (parent.has("width") && parent.has("height")) {
                arr[0] = parent.get("width").asInt();
                arr[1] = parent.get("height").asInt();
                return checkDimensions(arr);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

}
