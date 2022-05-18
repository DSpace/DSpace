package edu.umd.lib.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

public class EmbargoDTOServiceImpl implements EmbargoDTOService {

    private static Logger log = Logger.getLogger(EmbargoDTOServiceImpl.class);
    
    @Autowired(required = true)
    protected EmbargoDTODAO embargoDTODAO;
    
    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
    
    @Autowired(required = true)
    protected MetadataSchemaService metadataSchemaService;

    private static boolean fieldsInitialized = false;

    private static int titleId;
    private static int advisorId;
    private static int authorId;
    private static int departmentId;
    private static int typeId;
    private static String groupName;

    protected EmbargoDTOServiceImpl() {
        super();
    }

    @Override
    public List<EmbargoDTO> getEmbargoList(Context context) throws SQLException {
        if (!fieldsInitialized) {
            titleId = getDCFieldID(context, "title", null);
            advisorId = getDCFieldID(context, "contributor", "advisor");
            authorId = getDCFieldID(context, "contributor", "author");
            departmentId = getDCFieldID(context, "contributor", "department");
            typeId = getDCFieldID(context, "type", null);
            groupName = "ETD Embargo";
            
            fieldsInitialized = true;
        log.debug("Initializing metadata field IDs!");
        } else {
            log.debug("Metadata field IDs initialized already!");
        }
        
        return embargoDTODAO.getEmbargoDTOList(context, titleId, advisorId, authorId, departmentId, typeId, groupName);
    }
  
    private int getDCFieldID(Context context, String element, String qualifier) throws SQLException {
        MetadataField field = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, element, qualifier);
        if (field == null) {
            return -1;
        }
        return field.getID();
    }
}