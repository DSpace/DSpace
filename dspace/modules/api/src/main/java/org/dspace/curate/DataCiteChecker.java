package org.dspace.curate;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.identifier.DOIIdentifierProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 10/24/11
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DataCiteChecker extends AbstractCurationTask{

    private static final int NOT_SYNCH = 1;
    private static final int SYNCH = 0;

    String result="";

    @Override
    public int perform(DSpaceObject dso) throws IOException {

        if(!ConfigurationManager.getBooleanProperty("doi.datacite.connected", false)){
            this.setResult("Functionality not supported in test environment.");
            return Curator.CURATE_FAIL;
        }

        if(!(dso instanceof Item)) return Curator.CURATE_NOTASK;

        Item item = (Item)dso;

        // TODO TRY OUT Spring Configuraion
        CDLDataCiteService dataCiteService = new CDLDataCiteService(null, null);
        String doi = DOIIdentifierProvider.getDoiValue(item);

        // For local TEST!
        //doi = "10.5061/DRYAD.2222";

        String response = dataCiteService.lookup(doi);


        Map<String, String> mapResponse = parseResponse(response);
        if(mapResponse==null){
            this.setResult(result);
            return Curator.CURATE_FAIL;
        }

        if(verify(dataCiteService, item, mapResponse)==NOT_SYNCH)
            this.setResult("Attention! The Item is not synchronized!");
        else
            this.setResult("The Item is synchronized.");

        return Curator.CURATE_SUCCESS;

    }


    private  Map<String, String> parseResponse(String response){
        Map<String, String> map = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(response, "\n");
        String s = st.nextToken();

        if(response.contains("bad request") || response.contains("BAD REQUEST")){
            result=s;
            return null;
        }
        while(st.hasMoreElements()){
            String s1 = st.nextToken();
            map.put(s1.substring(0, s1.indexOf(':')), s1.substring(s1.indexOf(':')+2));
        }
        return map;
    }


    /**
     *
     * @param dataCiteService
     * @param item
     * @param response
     * @return NOT_SYNCH: if item's metadata are not synchronized with the metadata on DataCiteService
     *         CURATE_SUCCESS: if item's metadata are synchronized with the metadata on DataCiteService
     */
    private int verify(CDLDataCiteService dataCiteService, Item item, Map<String, String> response){
        Map<String, String> metadatalist = dataCiteService.createMetadataList(item);

        for(String key : metadatalist.keySet()){
            String valueItem = metadatalist.get(key);
            String valueDataCite = response.get(key);

            if(!valueItem.equals(valueDataCite)) return  NOT_SYNCH;
        }

        return SYNCH;
    }



}
