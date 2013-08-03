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
import org.dspace.core.Context;

/**
 * This class is the Oracle driver class for reading information about the authority use.
 * It implements the AuthorityDAO interface, and also has a
 * constructor of the form:
 *
 * AuthorityDAOPostgres(Context context)
 *
 * As required by AuthorityDAOFactory.  This class should only ever be loaded by
 * that Factory object.
 *
 * @author bollini
 */
public class AuthorityDAOOracle implements AuthorityDAO {
    private Context context;

    public AuthorityDAOOracle(Context context) {
        this.context = context;
    }

    public AuthorityInfo getAuthorityInfo(String md) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long countIssuedAuthorityKeys(String metadata) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ItemIterator findIssuedByAuthorityValue(String metadata, String authority) throws SQLException, AuthorizeException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterator findIssuedByAuthorityValueAndConfidence(
            String metadata, String authority, int confidence)
            throws SQLException, AuthorizeException, IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public AuthorityInfo getAuthorityInfoByAuthority(String authorityName)
			throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<String> listAuthorityKeyIssuedByAuthority(String authorityName,
			int limit, int page) throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public long countIssuedAuthorityKeysByAuthority(String authorityName)
			throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ItemIterator findIssuedByAuthorityValueInAuthority(
			String authorityName, String authority) throws SQLException,
			AuthorizeException, IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public long countIssuedItemsByAuthorityValueInAuthority(
			String authorityName, String key) throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String findNextIssuedAuthorityKeyInAuthority(String authorityName,
			String focusKey) throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String findPreviousIssuedAuthorityKeyInAuthority(
			String authorityName, String focusKey) throws SQLException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ItemIterator findIssuedByAuthorityValueAndConfidenceInAuthority(
			String authorityName, String authority, int confidence)
			throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
