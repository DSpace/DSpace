package edu.umd.lib.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;

import edu.umd.lib.dspace.content.EmbargoDTO;

public interface EmbargoDTODAO {

  List<EmbargoDTO> getEmbargoDTOList(Context context, int titleId, int advisorId, int authorId, int departmentId, int typeId, String groupName) throws SQLException;
  
}