package edu.umd.lims.dspace.app.webui.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.sql.SQLException;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.DCInputsReader;
import org.dspace.app.webui.util.DCInput;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.SubmissionInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.DCSeriesNumber;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

public class MainPageServlet extends org.dspace.app.webui.servlet.DSpaceServlet {

  protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
	 JSPManager.showJSP(request, response, "/index.jsp");
  }

  protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
	 JSPManager.showJSP(request, response, "/index.jsp");
  }

}
