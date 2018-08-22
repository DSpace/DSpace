/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess.service;

import org.dspace.content.*;
import static org.dspace.fileaccess.factory.FileAccessServiceFactory.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 13:17
 */
public interface ItemMetadataService {

    public String getPII(Item item);

    public String getDOI(Item item);

    public String getEID(Item item);

    public String getScopusID(Item item);

    public String getPubmedID(Item item);

    public enum IdentifierTypes{
        PII {
            @Override
            public String getIdentifier(Item item) {
                return getInstance().getItemMetadataService().getPII(item);
            }
        },
        DOI {
            @Override
            public String getIdentifier(Item item) {
                return getInstance().getItemMetadataService().getDOI(item);
            }
        },
        EID {
            @Override
            public String getIdentifier(Item item) {
                return getInstance().getItemMetadataService().getEID(item);
            }
        },
        SCOPUS_ID {
            @Override
            public String getIdentifier(Item item) {
                return getInstance().getItemMetadataService().getScopusID(item);
            }
        },
        PUBMED_ID {
            @Override
            public String getIdentifier(Item item) {
                return getInstance().getItemMetadataService().getPubmedID(item);
            }
        };

        public abstract String getIdentifier(Item item);
    }
}
