package org.dspace.content;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 22-sep-2010
 * Time: 11:15:27
 *
 * Class created to support external repository files
 */
public class DCRepositoryFile {

    private static Logger log = Logger.getLogger(DCRepositoryFile.class);

//    Map containing all the repository locations mapped on the label for quick retrieval
    public static Map<String, RepositoryLocation> repositoryLocations;
    public static List<String> repoConfigLocations;
    private String externalId;
    private String repository;

    static{
        repositoryLocations = new LinkedHashMap<String, RepositoryLocation>();

        try {
            //Read in our repo locations xml found in the dspace.cfg
            File repoLocFile = new File(ConfigurationManager.getProperty("dspace.dir") + "/config/repositoryLocations.xml");
            if(repoLocFile.exists()){
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document mainRepoNode = docBuilder.parse(new FileInputStream(repoLocFile));
                Node reposNode = mainRepoNode.getFirstChild();

                //Retrieve all our repositories
                NodeList repoNodes = XPathAPI.selectNodeList(reposNode, "//repo");
                for(int i = 0; i < repoNodes.getLength(); i++){
                    Node repoNode = repoNodes.item(i);
                    //Retrieve the information
                    String label = null;
                    String filter = null;
                    String contactUrl = null;

                    if(repoNode.getAttributes() != null){
                        NamedNodeMap attributes = repoNode.getAttributes();
                        if(attributes.getNamedItem("label") != null && attributes.getNamedItem("label").getNodeValue() != null)
                            label = attributes.getNamedItem("label").getNodeValue();

                        if(attributes.getNamedItem("filter") != null && attributes.getNamedItem("filter").getNodeValue() != null)
                            filter = attributes.getNamedItem("filter").getNodeValue();

                        if(attributes.getNamedItem("contact-url") != null && attributes.getNamedItem("contact-url").getNodeValue() != null)
                            contactUrl = attributes.getNamedItem("contact-url").getNodeValue();
                    }
                    if(label != null && filter != null && contactUrl != null)
                        repositoryLocations.put(label, new RepositoryLocation(label, filter, contactUrl));
                    else
                        log.warn("Warning repository location not loaded with label: " + label + " due to an incomplete xml node");
                }
            }
        } catch (Exception e) {
            log.error("Error while resolving the repository locations", e);
        }

        List<String> externalConfigRepos = new ArrayList<String>();
        String externalRepo;
        int index = 1;
        while((externalRepo = ConfigurationManager.getProperty("submit.repository.external." + index)) != null){
            if(!externalRepo.trim().equals(""))
                externalConfigRepos.add(externalRepo);
            index++;
        }
        repoConfigLocations =  externalConfigRepos;
    }

    /**
     * Contructor that creates a repository file object out of the given xml value
     * @param value a string representing an xml file
     */
    public DCRepositoryFile(String value){

        if(value.indexOf(":") != -1){
            repository= value.substring(0, value.lastIndexOf(":"));
            externalId = value.substring(value.lastIndexOf(":") + 1);
        }
        //TODO: log if externalid or repo == null
    }

    public DCRepositoryFile(String externalId, String repository) {
        this.externalId = externalId;
        this.repository = repository;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getRepository() {
        return repository;
    }

    public String toUrl(){
        RepositoryLocation repolocation = repositoryLocations.get(repository);
        if(repolocation != null)
            return repolocation.getUrl().replace("%externalresource%", externalId);

        //No url could be made, so just return null
        return null;
    }

    @Override
    public String toString() {
        if(repository != null && externalId != null)
            return repository + ":" + externalId;

        return null;
    }

    private static class RepositoryLocation {

        private String label;
        private String url;
        private String contactUrl;

        private RepositoryLocation(String label, String url, String contactUrl) {
            this.label = label;
            this.url = url;
            this.contactUrl = contactUrl;
        }

        public String getLabel() {
            return label;
        }

        public String getUrl() {
            return url;
        }

        public String getContactUrl() {
            return contactUrl;
        }
    }
}
