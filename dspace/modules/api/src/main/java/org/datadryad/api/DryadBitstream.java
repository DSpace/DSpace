/**
   Some tools for bitstreams that are specific to Dryad.
**/

package org.datadryad.api;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

import org.dspace.content.Bitstream;
import org.dspace.storage.bitstore.BitstreamStorageManager;

public class DryadBitstream {
    private static final Logger log = Logger.getLogger(DryadBitstream.class);
    private Bitstream bitstream;
    private String fileDescription = "<not initialized>";
    private String readmeFilename = null;
    
    public DryadBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    public Bitstream getDSpaceBitstream() {
        return bitstream;
    }

    public void setFileDescription(String description) {
        fileDescription = description;
    }
    
    public String getFileDescription() {
        return fileDescription;
    }

    public boolean isReadme() {
        String filename = bitstream.getName();
        if(filename.toLowerCase().startsWith("readme")) {
            return true;
        } else {
            return false;
        }
    }

    /*
      Assuming this bitstream is a README, set a special filename for it. The special filename
      will include "README_for_", the baseFilename passed in, and the file extension from the
      original README file that is stored in the DSpace bitstream.
    */
    public void setReadmeFilename(String baseFilename) {
        String origReadmeName = bitstream.getName();
        String readmeExtension = origReadmeName.substring(origReadmeName.lastIndexOf('.'), origReadmeName.length());
        baseFilename = baseFilename.substring(0, baseFilename.lastIndexOf('.'));
        readmeFilename = "README_for_" + baseFilename + readmeExtension;
    }

    public String getReadmeFilename() {
        return readmeFilename;
    }
    
    // Convenience method to access a properly serialized JSON string, formatted for use with DASH.
    public String getDashReferenceJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new SimpleModule().addSerializer(DryadBitstream.class, new DashSerializer()));
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            log.error("Unable to serialize Dash-style JSON", e);
            return "";
        }
    }
    
    /**
       Serializes this bitstream into a JSON object for use with DASH.
       Assumes the bitstream is stored in Amazon S3.
    **/
    public static class DashSerializer extends JsonSerializer<DryadBitstream> {
        @Override
        public void serialize(DryadBitstream dryadBitstream, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            Bitstream dspaceBitstream = dryadBitstream.getDSpaceBitstream();
            try {
                jGen.writeStartObject();

                String filename = dspaceBitstream.getName();
                if(dryadBitstream.isReadme() && (dryadBitstream.getReadmeFilename() != null)) {
                    filename = dryadBitstream.getReadmeFilename();
                }
                
                jGen.writeStringField("url", BitstreamStorageManager.getS3AccessURL(dspaceBitstream).toString());
                jGen.writeStringField("path", filename);
                jGen.writeNumberField("size", dspaceBitstream.getSize());
                jGen.writeStringField("description", dryadBitstream.getFileDescription());
                jGen.writeStringField("mimeType", dspaceBitstream.getFormat().getMIMEType());
                jGen.writeStringField("digest", dspaceBitstream.getChecksum());
                jGen.writeStringField("digestType", dspaceBitstream.getChecksumAlgorithm().toLowerCase());
                jGen.writeBooleanField("skipValidation", true);
                
                jGen.writeEndObject();
            } catch (Exception e) {
                throw new IOException("Unable to create DASH-formatted JSON for bitstream " + dspaceBitstream.getID(), e);                
            }
        }
    }
}
