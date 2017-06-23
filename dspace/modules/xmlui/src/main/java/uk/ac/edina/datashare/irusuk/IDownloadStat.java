package uk.ac.edina.datashare.irusuk;

import java.net.URL;
import java.util.Date;

public interface IDownloadStat {
    String getIPAddress();
    String getReferrer();
    String getUserAgent();
    String getOAIIdentifer();   
    Date getDate();
    URL getUrl();
}
