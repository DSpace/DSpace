package edu.umd.lib.dspace.content;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// import org.apache.log4j.Logger;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

public class EmbargoDTOServiceImpl implements EmbargoDTOService {

    // private static Logger log = Logger.getLogger(EmbargoDTOServiceImpl.class);

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
        // log.debug("Initializing metadata field IDs!");
        } else {
            // log.debug("Metadata field IDs initialized already!");
        }
        return embargoDTODAO.getEmbargoDTOList(context, titleId, advisorId, authorId, departmentId, typeId, groupName);

        //----------------
        // List<EmbargoDTO> embargoes = new ArrayList<>();
        // EmbargoDTO embargoDTO1 = new EmbargoDTO();
        // embargoDTO1.setAdvisor("Test1 Advisor");
        // embargoDTO1.setAuthor("Test1 Author");
        // embargoDTO1.setBitstreamId(UUID.randomUUID());
        // embargoDTO1.setDepartment("Test1 Department");
        // embargoDTO1.setEnddate(new Date(System.currentTimeMillis()));
        // embargoDTO1.setHandle("Test1 Handle");
        // embargoDTO1.setItemId(UUID.randomUUID());
        // embargoDTO1.setTitle("Test1 Title");
        // embargoDTO1.setType("Test1 Type");

        // EmbargoDTO embargoDTO2 = new EmbargoDTO();
        // embargoDTO2.setAdvisor("Test2 Advisor");
        // embargoDTO2.setAuthor("Test2 Author");
        // embargoDTO2.setBitstreamId(UUID.randomUUID());
        // embargoDTO2.setDepartment("Test2 Department");
        // embargoDTO2.setEnddate(new Date(System.currentTimeMillis()));
        // embargoDTO2.setHandle("Test2 Handle");
        // embargoDTO2.setItemId(UUID.randomUUID());
        // embargoDTO2.setTitle("Test2 Title");
        // embargoDTO2.setType("Test2 Type");

        // embargoes.add(embargoDTO1);
        // embargoes.add(embargoDTO2);
        // return embargoes;
        //----------------

    }

    private int getDCFieldID(Context context, String element, String qualifier) throws SQLException {
        MetadataField field = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(), element, qualifier);
        if (field == null) {
            return -1;
        }
        return field.getID();
    }
}