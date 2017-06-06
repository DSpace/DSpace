/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class StatisticsMigrator {
        private CSVReader csvRead;
        private CSVWriter csvWriter;
        private enum COL{owningColl,type,owningComm,id,owningItem;}
        private HashMap<COL,Integer> colIndex = new HashMap<>();
        private Context context;

        protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        
        public StatisticsMigrator(Context context, File f, File out) throws IOException {
                this.context = context;
                csvRead = new CSVReader(new FileReader(f));
                csvWriter = new CSVWriter(new FileWriter(out));
                String[] header = csvRead.readNext();
                if (header != null) {
                        for(int i=0; i<header.length; i++) {
                                for(COL col: COL.values()) {
                                        if (header[i].equals(col.name())) {
                                                colIndex.put(col, i);
                                                break;
                                        }
                                }
                        }
                }
                csvWriter.writeNext(header);
        }
        
        public void processFile() throws IOException, SQLException {
                for(String[] row = csvRead.readNext(); row != null; row = csvRead.readNext()) {
                        String comm = getColVal(row, COL.owningComm);
                        mapRowCol(row, COL.id);
                        mapRowCol(row, COL.owningComm);
                        mapRowCol(row, COL.owningColl);
                        mapRowCol(row, COL.owningItem);
                        csvWriter.writeNext(row);
                }
                csvWriter.close();
        }
        
        private String getColVal(String[] row, COL col){
                String val = null;
                Integer icol = colIndex.get(col);
                if (icol != null) {
                        if (icol < row.length) {
                                val = row[icol];
                        }
                }
                return val;
        }
        
        private Integer getInt(String val) {
                if (val == null) {
                        return null;
                }
                
                try {
                        return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                        return null;
                }
        }
        
        private void mapRowCol(String[] row, COL col) throws SQLException {
                Integer index = colIndex.get(col);
                if (index != null) {
                        if (index < row.length) {
                                String val = row[index];
                                if (col == COL.id) {
                                        String type = getColVal(row, COL.type);
                                        row[index] = mapIdVal(val, type);
                                } else {
                                        row[index] = mapVal(col, val);
                                }
                        }
                }
        }
        
        private String mapVal(COL col, String val) throws SQLException {
                if (val.contains(",")) {
                        StringBuilder sb = new StringBuilder();
                        for(String s: val.split(",")) {
                                if (sb.length() > 0) {
                                        sb.append(",");
                                }
                                sb.append(mapVal(col, s));
                        }
                        return sb.toString();
                }
                
                String ret = val;
                Integer ival = getInt(val);
                if (ival != null) {
                        UUID uuid = mapId(col, ival);
                        if (uuid != null) {
                                ret = uuid.toString();
                        }                                
                }
                return ret;
        }

        private String mapIdVal(String val, String type) throws SQLException {
                String ret = val;
                Integer ival = getInt(val);
                if (ival != null) {
                        UUID uuid = null;
                        Integer itype = getInt(type);
                        if (itype != null) {
                                uuid = mapType(itype, ival); 
                        }
                        if (uuid != null) {
                                ret = uuid.toString();
                        }                                
                }
                return ret;
        }

        private UUID mapId(COL col, int val) throws SQLException {
                if (col == COL.owningComm) {
                        Community comm = communityService.findByLegacyId(context, val);
                        return comm == null ? null : comm.getID();
                }
                if (col == COL.owningColl) {
                        Collection coll = collectionService.findByLegacyId(context, val);
                        return coll == null ? null : coll.getID();
                }
                if (col == COL.owningItem) {
                        Item item = itemService.findByLegacyId(context, val);
                        return item == null ? null : item.getID();
                }
                return null;
        }

        private UUID mapType(int type, int val) throws SQLException {
                if (type == Constants.COMMUNITY) {
                        Community comm = communityService.findByLegacyId(context, val);
                        return comm == null ? null : comm.getID();
                }
                if (type == Constants.COLLECTION) {
                        Collection coll = collectionService.findByLegacyId(context, val);
                        return coll == null ? null : coll.getID();
                }
                if (type == Constants.ITEM) {
                        Item item = itemService.findByLegacyId(context, val);
                        return item == null ? null : item.getID();
                }
                if (type == Constants.BITSTREAM) {
                        Bitstream bit = bitstreamService.findByLegacyId(context, val);
                        return bit == null ? null : bit.getID();
                }
                return null;
        }
}
