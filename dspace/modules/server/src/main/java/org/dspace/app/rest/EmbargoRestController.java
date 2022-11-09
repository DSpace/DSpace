package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.List;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RestController;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class EmbargoRestController {
  @Autowired
  private EmbargoDTOService embargoService;

  @PreAuthorize("hasAuthority('ADMIN')")
  @RequestMapping("/api/embargo-list")
  public List<EmbargoDTO> embargoList(HttpServletResponse response, HttpServletRequest request) throws SQLException {
    Context context = obtainContext(request);
    List<EmbargoDTO> embargoes = embargoService.getEmbargoList(context);

    return embargoes;
  }
}
