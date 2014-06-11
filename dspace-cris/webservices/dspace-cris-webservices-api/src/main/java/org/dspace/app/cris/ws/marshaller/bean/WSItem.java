/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.marshaller.bean;

import java.util.ArrayList;
import java.util.List;

public class WSItem
{
    private String handle;
    private int itemID;    
    private ArrayList<String> community;
    private ArrayList<String> communityName;
    private ArrayList<String> communityHandle;
    private ArrayList<String> collection;
    private ArrayList<String> collectionName;
    private ArrayList<String> collectionHandle;
    private List<WSMetadata> metadata = new ArrayList<WSMetadata>();
    public String getHandle() {
        return handle;
    }
    public void setHandle(String handle) {
        this.handle = handle;
    }
    public int getItemID() {
        return itemID;
    }
    public void setItemID(int itemID) {
        this.itemID = itemID;
    }
 
    public List<WSMetadata> getMetadata() {
        return metadata;
    }
    public void setMetadata(List<WSMetadata> metadata) {
        this.metadata = metadata;
    }
    public ArrayList<String> getCommunity()
    {
        return community;
    }
    public void setCommunity(ArrayList<String> community)
    {
        this.community = community;
    }
    public ArrayList<String> getCollection()
    {
        return collection;
    }
    public void setCollection(ArrayList<String> collection)
    {
        this.collection = collection;
    }
    public ArrayList<String> getCommunityName()
    {
        return communityName;
    }
    public void setCommunityName(ArrayList<String> communityName)
    {
        this.communityName = communityName;
    }
    public ArrayList<String> getCommunityHandle()
    {
        return communityHandle;
    }
    public void setCommunityHandle(ArrayList<String> communityHandle)
    {
        this.communityHandle = communityHandle;
    }
    public ArrayList<String> getCollectionName()
    {
        return collectionName;
    }
    public void setCollectionName(ArrayList<String> collectionName)
    {
        this.collectionName = collectionName;
    }
    public ArrayList<String> getCollectionHandle()
    {
        return collectionHandle;
    }
    public void setCollectionHandle(ArrayList<String> collectionHandle)
    {
        this.collectionHandle = collectionHandle;
    }
  
}
