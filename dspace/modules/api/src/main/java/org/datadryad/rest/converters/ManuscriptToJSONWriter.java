/*
 */
package org.datadryad.rest.converters;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptToJSONWriter {

    public static void main(String args[]) throws Exception {
        Manuscript manuscript = new Manuscript();
        manuscript.configureTestValues();
        writeJSON(manuscript, System.out);
    }

    static void writeJSON(Manuscript manuscript, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setConfig(mapper.getSerializationConfig().with(new SimpleDateFormat("yyyy-MM-dd")));
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(outputStream, manuscript);
    }

    
}
