/**
   Some tools for bitstreams that are specific to Dryad.
**/

package org.datadryad.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;

import org.dspace.content.Bitstream;

public class DryadBitstream {
    private Bitstream bitstream;
    
    public DryadBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    // Convenience method to access a properly serialized JSON string, formatted for use with DASH.
    public String getDashJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new SimpleModule().addSerializer(Bitstream.class, new Bitstream.DashSerializer()));
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
    public static class DashSerializer extends JsonSerializer<Package> {
        @Override
        public void serialize(Bitstream bitstream, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            try {
                jGen.writeStartObject();
                
                jGen.writeStringField("url", BitstreamStorageManager.getS3AccessURL(bitstream));
                jGen.writeStringField("path", bitstream.getName());
                jGen.writeStringField("description", bitstream.getFileDescription());
                jGen.writeStringField("mimeType", bitstream.getFormat().getMIMEType());
                
                jGen.writeEndObject();
            } catch (Exception e) {
                throw new IOException("Unable to create DASH-formatted JSON for bitstream " + bitstream.getID(), e);
                
            }
        }
    }
}
