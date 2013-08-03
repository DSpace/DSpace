/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.ItemIterator;


/**
 * Interface for any class wishing to investigate the current use of authority for
 * Read Only operations.  If you wish to modify the contents of a metadata
 * (authority key, confidence, etc.) you need to use the Item API.
 *
 * If you implement this class, and you wish it to be loaded via the AuthorityDAOFactory
 * you must supply a constructor of the form:
 *
 * public AuthorityDAOImpl(Context context) {}
 *
 * Where Context is the DSpace Context object
 *
 * @author bollini
 */
public interface AuthorityDAO {

    public AuthorityInfo getAuthorityInfo(String md) throws SQLException;

    public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException;

    public long countIssuedAuthorityKeys(String metadata) throws SQLException;

     /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field and a confidence level not acceptable.
     *
     * @see Choices#CF_ACCEPTED
     * @param context DSpace context object
     * @param metadata metadata field schema.element.qualifier
     * @param authority the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     */
    public ItemIterator findIssuedByAuthorityValue(String metadata,
            String authority) throws SQLException, AuthorizeException, IOException;

    public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException;

    public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException;

    public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException;
    
    public ItemIterator findIssuedByAuthorityValueAndConfidence(String metadata,
            String authority, int confidence) throws SQLException, AuthorizeException, IOException;
    
    
    /*
     *	Methods for query an authority about all metadata binded to it  
     */
    
    public AuthorityInfo getAuthorityInfoByAuthority(String authorityName) throws SQLException;

    public List<String> listAuthorityKeyIssuedByAuthority(String authorityName, int limit, int page) throws SQLException;

    public long countIssuedAuthorityKeysByAuthority(String authorityName) throws SQLException;

    public ItemIterator findIssuedByAuthorityValueInAuthority(String authorityName,
            String authority) throws SQLException, AuthorizeException, IOException;

    public long countIssuedItemsByAuthorityValueInAuthority(String authorityName, String key) throws SQLException;

    public String findNextIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException;

    public String findPreviousIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException;
    
    public ItemIterator findIssuedByAuthorityValueAndConfidenceInAuthority(String authorityName,
            String authority, int confidence) throws SQLException, AuthorizeException, IOException;
}
