/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.core.Context;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.Record;
import org.apache.http.HttpException;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public interface SubmissionLookupDataLoader extends DataLoader
{

    public final static String DOI = "doi";

    public final static String PUBMED = "pubmed";

    public final static String ARXIV = "arxiv";

    public final static String REPEC = "repec";

    public final static String SCOPUSEID = "scopuseid";

    public final static String CINII = "cinii";

    public final static String TYPE = "type";

    List<String> getSupportedIdentifiers();

    boolean isSearchProvider();

    List<Record> search(Context context, String title, String author, int year)
            throws HttpException, IOException;

    List<Record> getByIdentifier(Context context, Map<String, Set<String>> keys)
            throws HttpException, IOException;

    List<Record> getByDOIs(Context context, Set<String> doiToSearch)
            throws HttpException, IOException;

}
