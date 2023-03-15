package edu.umd.lib.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import edu.umd.lib.dspace.content.EmbargoDTO;
import org.dspace.core.Context;

/**
 * Interface for the data access object of embargoed items for data transfer.
 */
public interface EmbargoDTODAO {
    /**
     * Returns a list of embargoed items.
     *
     * @param context the application context
     * @param titleId the MetadataField id for the title field
     * @param advisorId the MetadataField id for the advisor field
     * @param authorId the MetadataField id for the author field
     * @param departmentId the MetadataField id for the department field
     * @param typeId the MetadataField id for the type field
     * @param groupName the group name of embargoed items
     * @return a list of embargoed items.
     * @throws SQLException if a database error occurs.
     */
    List<EmbargoDTO> getEmbargoDTOList(
        Context context, int titleId, int advisorId, int authorId, int departmentId,
            int typeId, String groupName) throws SQLException;
}
