package edu.umd.lib.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

public class EmbargoDTOServiceImpl implements EmbargoDTOService {

 
  @Autowired(required = true)
  protected EmbargoDTODAO embargoDTODAO;
  
  @Autowired(required = true)
  protected static MetadataFieldService metadataFieldService;
  
  @Autowired(required = true)
  protected static MetadataSchemaService metadataSchemaService;

  private static boolean fieldsInitialized = false;

  private static int titleId;
  private static int advisorId;
  private static int authorId;
  private static int departmentId;
  private static int typeId;
  private static int resourceType;
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
          resourceType = Constants.ITEM;
          groupName = "ETD Embargo";

          fieldsInitialized = true;
      }

    return embargoDTODAO.getEmbargoDTOList(context, titleId, advisorId, authorId, departmentId, typeId, resourceType, groupName);
  }

  private static int getDCFieldID(Context context, String element, String qualifier) throws SQLException {
    return metadataFieldService.findByElement(
                context, MetadataSchema.DC_SCHEMA, element, qualifier).getID();
  }
}