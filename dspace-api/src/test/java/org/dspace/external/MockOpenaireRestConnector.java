/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.InputStream;

import eu.openaire.jaxb.helper.OpenAIREHandler;
import eu.openaire.jaxb.model.Response;

/**
 * Mock the Openaire rest connector for unit testing<br>
 * will be resolved against static test xml files
 * 
 * @author pgraca
 *
 */
public class MockOpenaireRestConnector extends OpenaireRestConnector {

    public MockOpenaireRestConnector(String url) {
        super(url);
    }

    @Override
    public Response searchProjectByKeywords(int page, int size, String... keywords) {
        try {
            return OpenAIREHandler.unmarshal(this.getClass().getResourceAsStream("openaire-projects.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response searchProjectByIDAndFunder(String projectID, String projectFunder, int page, int size) {
        try {
            return OpenAIREHandler.unmarshal(this.getClass().getResourceAsStream("openaire-project.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response search(String path, int page, int size) {
        try {
            return OpenAIREHandler.unmarshal(this.getClass().getResourceAsStream("openaire-no-projects.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public InputStream get(String file, String accessToken) {
        return this.getClass().getResourceAsStream("openaire-no-projects.xml");
    }
}
