/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;
import java.sql.SQLException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 *
 * @author moubarik
 */
public class StubItem implements Serializable{
    private int owningCollectionId;
    private String[] collections;
    private String[] bundles;
    private boolean in_archive;
    private boolean withdrawn;
    private DCValue[] metadata;
    private int Id;
    
    public StubItem(Item i) throws SQLException{
        this.Id = i.getID();
        if(i.getBundles() != null){
            this.bundles = new String[i.getBundles().length];
            for(int j = 0; j < this.bundles.length; j++){
                this.bundles[j] = i.getBundles()[j].getName();
            }
        }
        if(i.getCollections() != null){
            this.collections = new String[i.getCollections().length];
            for(int j = 0; j < this.collections.length; j++){
                this.collections[j] = i.getCollections()[j].getName();
            }
        }
        if(i.getOwningCollection() != null){
            this.owningCollectionId = i.getOwningCollection().getID();
        }
        this.in_archive = i.isArchived();
        this.withdrawn = i.isWithdrawn();
        this.metadata = i.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    }
    
    public int getOwningCollectionID(){
        return owningCollectionId;
    }
    
    public String[] getCollections(){
        return collections;
    }
    
    public String[] getBundles(){
        return bundles;
    }
    
    public boolean in_archive(){
        return in_archive;
    }
    
    public boolean withdrawn(){
        return withdrawn;
    }

    public int getOwningCollectionId() {
        return owningCollectionId;
    }

    public void setOwningCollectionId(int owningCollectionId) {
        this.owningCollectionId = owningCollectionId;
    }

    public boolean isIn_archive() {
        return in_archive;
    }

    public void setIn_archive(boolean in_archive) {
        this.in_archive = in_archive;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
    }

    public DCValue[] getMetadata() {
        return metadata;
    }

    public void setMetadata(DCValue[] metadata) {
        this.metadata = metadata;
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }
    
}
