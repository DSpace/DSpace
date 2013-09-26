package org.dspace.submit.importer;

import org.dspace.eperson.EPerson;

public interface Importer
{
    public ImportResultBean ingest(String data, EPerson eperson);
}
