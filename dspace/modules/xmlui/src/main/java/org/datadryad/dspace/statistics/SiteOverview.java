package org.datadryad.dspace.statistics;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class SiteOverview extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(SiteOverview.class);
    private SourceValidity validity;

    // private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_type         = message("org.datadryad.dspace.statistics.type");          // "Type"
    private static final Message T_total        = message("org.datadryad.dspace.statistics.total");         // "Total"
    private static final Message T_thirtydays   = message("org.datadryad.dspace.statistics.thirtydays");    // "30 days"
    private static final Message T_datapackages = message("org.datadryad.dspace.statistics.datapackges");   // "Data packages"
    private static final Message T_datafiles    = message("org.datadryad.dspace.statistics.datafiles");     // "Data files"
    private static final Message T_journals     = message("org.datadryad.dspace.statistics.journals");      // "Journals"
    private static final Message T_authors      = message("org.datadryad.dspace.statistics.authors");       // "Authors"
    private static final Message T_downloads    = message("org.datadryad.dspace.statistics.downloads");     // "Downloads"
    
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException 
    {
        SiteOverviewStats stats;
        try {
            stats = new SiteOverviewStats(context);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new UIException("Failed to create stats overview object");
        }

        Division overviewStats = body.addDivision("front-page-stats");
        org.dspace.app.xmlui.wing.element.Table infoTable = overviewStats.addTable("list-table",5,3);
        
        Row headerRow = infoTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent(T_type);
        headerRow.addCell().addContent(T_total);
        headerRow.addCell().addContent(T_thirtydays); 

        Row row = infoTable.addRow();
        row.addCell("data").addContent(T_datapackages);
        row.addCell("data").addContent(stats.getDataPackageCount());
        row.addCell("data").addContent(stats.getDataPackageCount_30day());

        row = infoTable.addRow();
        row.addCell("data").addContent(T_datafiles);
        row.addCell("data").addContent(stats.getDataFileCount());
        row.addCell("data").addContent(stats.getDataFileCount_30day());

        row = infoTable.addRow();
        row.addCell("data").addContent(T_journals);
        row.addCell("data").addContent(stats.getJournalCount());
        row.addCell("data").addContent(stats.getJournalCount_30day());

        row = infoTable.addRow();
        row.addCell("data").addContent(T_authors);
        row.addCell("data").addContent(stats.getUniqAuthors());
        row.addCell("data").addContent(stats.getUniqAuthors_30day());

        row = infoTable.addRow();
        row.addCell("data").addContent(T_downloads);
        row.addCell("data").addContent(stats.getTotalFileDownload());
        row.addCell("data").addContent(stats.getTotalFileDownload_30day());
    }

    //	@Override
    public Serializable getKey() {
        return getClass().getName();
    }

    //	@Override
    public SourceValidity getValidity() {
        if (validity == null) {
            DSpaceValidity newValidity = new DSpaceValidity();
            newValidity.setAssumedValidityDelay(86400000);
            validity = (SourceValidity) newValidity.complete();
        }
        return validity;
    }
}
