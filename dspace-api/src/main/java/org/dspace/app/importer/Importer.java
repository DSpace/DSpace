package org.dspace.app.importer;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.eperson.EPerson;

public interface Importer
{
    public ImportResultBean ingest(String data, EPerson eperson);
    public ImportResultBean ingest(String data, Community com, EPerson eperson);
    public ImportResultBean ingest(String data, Collection coll, EPerson eperson);
}
