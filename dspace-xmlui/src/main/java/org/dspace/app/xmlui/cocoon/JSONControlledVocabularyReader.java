/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import com.google.gson.Gson;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.vocabulary.ControlledVocabulary;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Reader that parses a given controlled vocabulary and returns a json representation
 * The json this class returns will be parsed in the vocabulary-support.js file.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class JSONControlledVocabularyReader extends AbstractReader {

    private static final Logger log = Logger.getLogger(JSONControlledVocabularyReader.class);

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String vocabularyIdentifier = request.getParameter("vocabularyIdentifier");
        try {
            ControlledVocabulary controlledVocabulary = ControlledVocabulary.loadVocabulary(vocabularyIdentifier);
            if(controlledVocabulary != null){
                Gson gson = new Gson();

                String jsonString = gson.toJson(controlledVocabulary);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
                IOUtils.copy(inputStream, out);
                out.flush();
            }
            out.close();
        } catch (Exception e) {
            log.error("Error while generating controlled vocabulary json, vocabulary identifier: " + vocabularyIdentifier, e);
        }
    }
}
