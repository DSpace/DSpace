/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;
import org.dspace.content.Bitstream;

/**
 *
 * @author moubarik
 */
public class StubBundle implements Serializable{
    private int id;
    private String bundleName;
    private int primarybitstreamid;
    private String[] bitstreamUrls;

    public StubBundle(int id, String bundleName, int primarybitstreamid, Bitstream[] bitstreams) {
        this.id = id;
        this.bundleName = bundleName;
        this.primarybitstreamid = primarybitstreamid;
        bitstreamUrls = new String[bitstreams.length];
        for(int i = 0; i < bitstreams.length; i++){
            bitstreamUrls[i] = "bitstreams/" + bitstreams[i].getID();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public int getPrimarybitstreamid() {
        return primarybitstreamid;
    }

    public void setPrimarybitstreamid(int primarybitstreamid) {
        this.primarybitstreamid = primarybitstreamid;
    }

    public String[] getBitstreamUrls() {
        return bitstreamUrls;
    }

    public void setBitstreamUrls(String[] bitstreamUrls) {
        this.bitstreamUrls = bitstreamUrls;
    }
    
}
