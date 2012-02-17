/*
 * 
 * Created on Dec 24, 2011
 * Last updated on Dec 30, 2011
 * 
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Suspendable
public class NCBILinkOutBuilder extends AbstractCurationTask{
    
    Map<String,Set<String>> articleDataMap = null;  //articleDOI -> set of datapackageDOIs

    List<String>linkOutDBNameList = null;
    List<String>linkOutDBAbbrevList = null;
    
//    static Logger logger = Logger.getLogger(NCBILinkOutBuilder.class.getName());
    private Context dspaceContext;
    private static Logger LOGGER = LoggerFactory.getLogger(NCBILinkOutBuilder.class);

    static final String NCBIEntrezPrefix = "";
    
    static final String NCBIDatabasePrefix = "http://www.ncbi.nlm.nih.gov/";
    

    

    @Override
    public void init(Curator curator, String taskID) throws IOException{
        super.init(curator, taskID);
        
        try {
            dspaceContext = new Context();
        } catch (SQLException e1) {
            LOGGER.error("Unable to create Dspace context");
            LOGGER.error("Exception was " + e1);
            return;
        }

        // init article to data package mapping
        articleDataMap = new HashMap<String,Set<String>>();
        
        // init lists of databases that support linkout
        linkOutDBNameList = new ArrayList<String>();
        linkOutDBAbbrevList = new ArrayList<String>();
        
        linkOutDBNameList.add("gene");
        linkOutDBAbbrevList.add("Gene");
        
        linkOutDBNameList.add("nucleotide");
        linkOutDBAbbrevList.add("Nucleotide");
        
        linkOutDBNameList.add("est");
        linkOutDBAbbrevList.add("NucEST");
        
        linkOutDBNameList.add("gss");
        linkOutDBAbbrevList.add("NucGSS");
        
        linkOutDBNameList.add("pubmed");
        linkOutDBAbbrevList.add("PubMed");
        
        linkOutDBNameList.add("protein");
        linkOutDBAbbrevList.add("Protein");
    
        linkOutDBNameList.add("taxonomy");
        linkOutDBAbbrevList.add("Taxonomy");

        linkOutDBNameList.add("bioproject");
        linkOutDBAbbrevList.add("BioProject");

    }


    
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso instanceof Collection){
            distribute(dso);
            return Curator.CURATE_SUCCESS;
            
        }
        return Curator.CURATE_SKIP;
    }
    
    
    @Override
    protected void performItem(Item item){
        final String handle = item.getHandle();
        final StringBuilder result = new StringBuilder(1000);
        String articleDOI = "[no DOI found]";
        String dataDOIs[];
        DCValue partof[] = item.getMetadata("dc.relation.ispartof");
        if (handle != null && partof == null){  //article that is not workflow
            DCValue[] vals = item.getMetadata("dc.identifier");
            if (vals.length > 0){
                for(int i = 0; i< vals.length; i++){
                    if (vals[i].value.startsWith("doi")){
                        articleDOI = vals[i].value;
                    }
                }
            }
            DCValue parts[] = item.getMetadata("dc.relation.partof");
            if (parts.length == 0){
                this.report("Article " + articleDOI + " reported no parts");
            }
        }
    }

}
