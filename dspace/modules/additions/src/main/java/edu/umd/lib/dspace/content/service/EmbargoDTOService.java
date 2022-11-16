package edu.umd.lib.dspace.content.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;

import edu.umd.lib.dspace.content.EmbargoDTO;

/**
 * Service interface class for the EmbargoDTO object.
 *
 * The implementation of this class is responsible for all business logic calls
 * for the EmbargoDTO object and is autowired by spring
 */
public interface EmbargoDTOService {
    /**
     * Return the list of embargoed items.
     *
     * @return embargoList List of EmbargoDTO object
     */
    public List<EmbargoDTO> getEmbargoList(Context context) throws SQLException;
}
