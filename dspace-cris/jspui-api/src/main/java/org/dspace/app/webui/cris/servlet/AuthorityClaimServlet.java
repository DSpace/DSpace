package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.dspace.app.cris.integration.BindItemToRP;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.util.Util;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryViewAndHighlightConfiguration;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

import it.cilea.osd.common.constants.Constants;

public class AuthorityClaimServlet extends DSpaceServlet
{
    
    private static final String UNCLAIM_ACTION = "unclaim";

    private Logger log = Logger.getLogger(AuthorityClaimServlet.class);
    
    private static final String[] METADATA_MESSAGE = new String[] { "local",
            "message", "claim" };

    private static final String PUBLICATION_CLAIMED= "publication-claimed";
    
    private static final String PUBLICATION_REJECTED = "publication-rejected";

    private static final String PUBLICATION_CLAIM_REQUEST = "publication-claim-request-received";
    
    private static final String PUBLICATION_CLAIM_REVIEW = "publication-claim-request-review";
    
    private static final String SUBMIT_DISCARD = "submit_reject";
    private static final String SUBMIT_CLAIM = "submit_approve";
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private DSpace dspace = new DSpace();

    private static final boolean forceCommit = ConfigurationManager
            .getBooleanProperty(CrisConstants.CFG_MODULE, "publication.claim.list.solr.force.commit", false);

    private static final String checksimilarityString = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "publication.claim.list.checksimilarity");
    
    private ApplicationService applicationService = dspace.getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);

    private IndexingService indexer = dspace.getServiceManager()
            .getServiceByName(IndexingService.class.getName(),
                    IndexingService.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String action = request.getParameter("action");

        String handle = request.getParameter("handle");
        String crisID = (String) request
                .getAttribute("requesterMapPublication");
        String metadata = request.getParameter("metadata");
        
        // find list of match
        DiscoveryViewAndHighlightConfiguration discoveryViewAndHighlightConfigurationByName = SearchUtils
                .getDiscoveryViewAndHighlightConfigurationByName("global");

        request.setAttribute("viewMetadata",
                discoveryViewAndHighlightConfigurationByName
                        .getViewConfiguration());
        request.setAttribute("selectorViewMetadata",
                discoveryViewAndHighlightConfigurationByName.getSelector());

        if (StringUtils.isBlank(crisID))
        {
            crisID = context.getCrisID();
        }
        
        
        if(UNCLAIM_ACTION.equals(action)) {
            unclaim(context, request, response, handle, crisID, metadata);
        }
        else {
            List<Item> publications = (List<Item>) request
                    .getAttribute("publicationList");
            if (publications != null)
            {
                showPossibleMatch(context, request, response, crisID,
                        publications, metadata);
            }
            else
            {
                Map<String, List<String[]>> result = new HashMap<String, List<String[]>>();
                Map<String, Boolean> haveSimilar = new HashMap<String, Boolean>();
                showClaimPublication(context, request, response, handle, crisID,
                        result, haveSimilar, metadata);
            }
        }
    }

    private void unclaim(Context context, HttpServletRequest request,
            HttpServletResponse response, String handle, String crisID,
            String metadata)
            throws SQLException, AuthorizeException, IOException
    {
        if(StringUtils.isNotBlank(metadata)) {
            context.turnOffAuthorisationSystem();       
            // retrieve Group users to send notification; Default is Administrator
            // Group
            String notifyGroupSelfClaim = ConfigurationManager.getProperty("cris",
                    "notify-publication.claim.group.name");
            if (StringUtils.isBlank(notifyGroupSelfClaim))
            {
                notifyGroupSelfClaim = "Administrator";
            }

            String[] splitted = Utils.tokenize(metadata);
            String schema = splitted[0];
            String element = splitted[1];
            String qualifier = splitted[2];
            
            Item item = (Item)HandleManager.resolveToObject(context, handle);
            
            String templateEmailParamMetadataValue = "";
            String templateEmailParamMetadataAuthority = "";
            String templateEmailParamMetadataConfidence = "";
            
            Metadatum[] mmm = item.getMetadataByMetadataString(metadata);
            item.clearMetadata(schema, element, qualifier, Item.ANY);
            for(Metadatum mm : mmm) {
                if(crisID.equals(mm.authority)) {
                    templateEmailParamMetadataValue = mm.value;
                    templateEmailParamMetadataAuthority = mm.authority;
                    templateEmailParamMetadataConfidence = ""+mm.confidence;
                    item.addMetadata(schema, element, qualifier,
                            mm.language, mm.value, null, Choices.CF_UNSET);
                }
                else {
                    item.addMetadata(schema, element, qualifier,
                            mm.language, mm.value, mm.authority, mm.confidence);
                }
            }
            item.update();
            context.commit();
            
            sendEmail(context, PUBLICATION_REJECTED, notifyGroupSelfClaim,
                    context.getCurrentUser().getEmail(), metadata,
                    templateEmailParamMetadataValue, templateEmailParamMetadataAuthority,
                    templateEmailParamMetadataConfidence, "None", handle, crisID, context.getCurrentUser(), item.getName());
            
            response.sendRedirect(
                    request.getContextPath() + "/handle/" + handle);
            context.restoreAuthSystemState();
        }
    }

    private void showPossibleMatch(Context context, HttpServletRequest request,
            HttpServletResponse response, String crisID,
            List<Item> publications, String metadata)
            throws SQLException, ServletException, IOException
    {
        Map<String, Map<String, List<String[]>>> mapResult = new HashMap<String, Map<String, List<String[]>>>();
        Map<String, Map<String, Boolean>> haveSimilarResult = new HashMap<String, Map<String, Boolean>>();
        Map<String, DSpaceObject> mapItem = new HashMap<String, DSpaceObject>();
        ResearcherPage rp = applicationService.getEntityByCrisId(crisID,
                ResearcherPage.class);
        for (Item ii : publications)
        {
            Map<String, List<String[]>> result = new HashMap<String, List<String[]>>();
            Map<String, Boolean> haveSimilar = new HashMap<String, Boolean>();
            String handle = ii.getHandle();
            doResult(context, handle, crisID, result, haveSimilar, rp, metadata);
            mapResult.put(handle, result);
            mapItem.put(handle, HandleManager.resolveToObject(context, handle));
            haveSimilarResult.put(handle, haveSimilar);
        }

        log.info(LogManager.getHeader(context, "show_authority_claim_list",
                null));
        request.setAttribute("items", mapItem);
        request.setAttribute("result", mapResult);
        request.setAttribute("haveSimilar", haveSimilarResult);
        request.setAttribute("crisID", crisID);
        request.setAttribute("checksimilarity", checksimilarityString);

        JSPManager.showJSP(request, response,
                "/tools/authority-claim-list.jsp");

    }

    private void showClaimPublication(Context context, HttpServletRequest request,
            HttpServletResponse response, String handle, String crisID,
            Map<String, List<String[]>> result,
            Map<String, Boolean> haveSimilar, String metadata)
            throws SQLException, ServletException, IOException
    {

        ResearcherPage rp = applicationService.getEntityByCrisId(crisID,
                ResearcherPage.class);

        doResult(context, handle, crisID, result, haveSimilar, rp, metadata);

        request.setAttribute("item",
                HandleManager.resolveToObject(context, handle));
        request.setAttribute("result", result);
        request.setAttribute("handle", handle);
        request.setAttribute("haveSimilar", haveSimilar);
        request.setAttribute("crisID", crisID);
        request.setAttribute("checksimilarity", checksimilarityString);
        
        log.info(LogManager.getHeader(context, "show_authority_claim",
                "#keys: " + result.size()));

        JSPManager.showJSP(request, response, "/tools/authority-claim.jsp");
    }

    private void doResult(Context context, String handle, String crisID,
            Map<String, List<String[]>> result,
            Map<String, Boolean> haveSimilar, ResearcherPage rp, String metadata)
            throws SQLException
    {
        //alghoritm used to calculate the similarity (Default)
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        double checksimilarity = Double.parseDouble(checksimilarityString);
        
        if (StringUtils.isNotBlank(handle))
        {

            Item item = (Item) (HandleManager.resolveToObject(context, handle));

            List<MetadataField> metadataFields = BindItemToRP
                    .metadataFieldWithAuthorityRP(context);
            general : for (MetadataField metadataField : metadataFields)
            {

                MetadataSchema find = MetadataSchema.find(context,
                        metadataField.getSchemaID());
                String field = Utils.standardize(find.getName(),
                        metadataField.getElement(),
                        metadataField.getQualifier(), "_");
                String standardizeField = Utils.standardize(find.getName(),
                        metadataField.getElement(),
                        metadataField.getQualifier(), ".");
                
                haveSimilar.put(field, false);
                
                if(StringUtils.isNotBlank(metadata)) {
                    if(standardizeField.equals(metadata)) {
                        
                        prepareAndFindSimilarity(crisID, result, haveSimilar, rp,
                                jaroWinklerDistance, checksimilarity, item, field,
                                standardizeField);
                        break general;
                    }
                }
                else {
                    
                    prepareAndFindSimilarity(crisID, result, haveSimilar, rp,
                            jaroWinklerDistance, checksimilarity, item, field,
                            standardizeField);
                }


            }

        }
    }

    private void prepareAndFindSimilarity(String crisID, Map<String, List<String[]>> result,
            Map<String, Boolean> haveSimilar, ResearcherPage rp,
            JaroWinklerDistance jaroWinklerDistance, double checksimilarity,
            Item item, String field, String standardizeField)
    {
        Metadatum[] metadatum = item
                .getMetadataByMetadataString(standardizeField);

        for (Metadatum meta : metadatum)
        {
            String similar = null;
            choice: for (String allname : rp.getAllNames())
            {
                if (crisID.equals(meta.authority)
                        || allname.equals(meta.value)
                        || jaroWinklerDistance.getDistance(allname,
                                meta.value) > checksimilarity
                        || allname.startsWith(meta.value)
                        || meta.value.startsWith(allname))
                {
                    similar = meta.value;
                    haveSimilar.put(field, true);
                    break choice;
                }
            }

            List<String[]> options = null;
            if (result.containsKey(field))
            {
                options = result.get(field);
            }
            else
            {
                options = new ArrayList<String[]>();
            }

            String[] innerOptions = new String[] { meta.value,
                    meta.authority, "" + meta.confidence, meta.language,
                    similar };
            options.add(innerOptions);
            result.put(field, options);
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();

        final Date now = new Date();

        final String submitButton = UIUtil.getSubmitButton(request,
                "submit_cancel");

        String handle = request.getParameter("handle");
        String crisID = context.getCrisID();

        String notifyGroupSelfClaim = getSelfClaimGroup();

        boolean selfClaim = isMemberOfSelfClaimGroup(context);

        if (!"submit_cancel".equals(submitButton))
        {
            claim(context, request, now, submitButton, crisID,
                    notifyGroupSelfClaim, selfClaim);
        }

        if (StringUtils.isBlank(handle))
        {
            response.sendRedirect(
                    request.getContextPath() + "/cris/rp/" + crisID);
        }
        else
        {
            response.sendRedirect(
                    request.getContextPath() + "/handle/" + handle);
        }
        context.restoreAuthSystemState();
    }

    private String getSelfClaimGroup()
    {
        // retrieve Group users to send notification; Default is Administrator
        // Group
        String notifyGroupSelfClaim = ConfigurationManager.getProperty("cris",
                "notify-publication.claim.group.name");
        if (StringUtils.isBlank(notifyGroupSelfClaim))
        {
            notifyGroupSelfClaim = "Administrator";
        }
        return notifyGroupSelfClaim;
    }

    private boolean isMemberOfSelfClaimGroup(Context context) throws SQLException
    {
        // check if currentUser is member of the self claim group
        boolean selfClaim = false;
        String nameGroupSelfClaim = ConfigurationManager.getProperty("cris",
                "publication.claim.group.name");
        if (StringUtils.isNotBlank(nameGroupSelfClaim))
        {
            Group selfClaimGroup = Group.findByName(context,
                    nameGroupSelfClaim);
            if (selfClaimGroup != null)
            {
                if (Group.isMember(context, selfClaimGroup.getID()))
                {
                    selfClaim = true;
                }
            }
        }
        return selfClaim;
    }

    private void claim(Context context, HttpServletRequest request,
            final Date now, final String submitButton, String crisID,
            String notifyGroupSelfClaim, boolean selfClaim)
    {
        int[] selectedIds = UIUtil.getIntParameters(request, "selectedId");

        String message = null;
        int failures = 0;
        int successes = 0;
        int discarded = 0;
        for (int selectedId : selectedIds)
        {
            try
            {
                String selectedHandle = request
                        .getParameter("handle_" + selectedId);
                workNow(context, request, now, selectedHandle, crisID,
                        notifyGroupSelfClaim, selfClaim, selectedId,
                        submitButton);
                if (SUBMIT_CLAIM.equalsIgnoreCase(submitButton))
                {
                    successes++;
                }
                else
                {
                    discarded++;
                }
            }
            catch (Exception ex)
            {
                failures++;
                log.error(ex.getMessage(), ex);
            }
        }

        if (failures > 0)
        {
            if (SUBMIT_CLAIM.equalsIgnoreCase(submitButton))
            {
                message = I18nUtil.getMessage(
                        "jsp.dspace.authority-listclaim.failure.success",
                        new Object[] { successes, failures },
                        context.getCurrentLocale(), false);
            }
            else
            {
                message = I18nUtil.getMessage(
                        "jsp.dspace.authority-listclaim.failure.reject",
                        new Object[] { discarded, failures },
                        context.getCurrentLocale(), false);
            }
        }
        else
        {
            if (successes > 0)
            {
                message = I18nUtil.getMessage(
                        "jsp.dspace.authority-listclaim.success",
                        new Object[] { successes },
                        context.getCurrentLocale(), false);
            }
            else
            {
                message = I18nUtil.getMessage(
                        "jsp.dspace.authority-listclaim.reject",
                        new Object[] { selectedIds.length },
                        context.getCurrentLocale(), false);
            }
        }
        if (StringUtils.isNotBlank(message))
        {
            request.getSession().setAttribute(Constants.MESSAGES_KEY,
                    Arrays.asList(message));
        }
        if (forceCommit)
        {
            try
            {
                indexer.commit();
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void workNow(Context context, HttpServletRequest request,
            final Date now, String handle, String crisID,
            String notifyGroupSelfClaim, boolean selfClaim, int selectedId,
            String submitMode) throws SQLException, AuthorizeException
    {
        
        boolean gotoReview = false;
        
        //the metadata field key
        String templateEmailParamFieldKey = null;
        //the metadata value
        String templateEmailParamMetadataValue = null;
        //the metadata authority 
        String templateEmailParamMetadataAuthority = null;
        //the metadata confidence
        String templateEmailParamMetadataConfidence = null;
        //the user form note (if exists otherwise retrieved by i18n file)
        String templateEmailParamUserNote = null;
        
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();

        List<String> choices = new ArrayList<String>();

        // find selected person
        findSelectedPersons(request, selectedId, choices);
        
        Item item = (Item) HandleManager.resolveToObject(context,
                handle);
        
        // for each publication try to accept/reject
        for (String choice : choices)
        {
            if(StringUtils.isNotBlank(choice)) {
                String[] arrayChoices = null;
                arrayChoices = choice.split("_", 2);
    
                // the choice sequence by the end user -> 00
                String sequenceChoice = arrayChoices[0];
                // the field with the identifier as prefix ->
                // 16_dc_contributor_author
                String fieldChoice = arrayChoices[1];
    
                // try to retrieve the text note ->
                // requestNote_16_dc_contributor_author
                templateEmailParamUserNote = request.getParameter("requestNote_" + fieldChoice);
    
                Set<Integer> itemRejectedIDs = new HashSet<Integer>();
                if (StringUtils.isNotBlank(fieldChoice))
                {
                    if (SUBMIT_DISCARD.equalsIgnoreCase(submitMode))
                    {
                        itemRejectedIDs.add(item.getID());
                    }
                    else
                    {
                        String[] metadata = fieldChoice.split("_");
                        // skip item id ---> e.g. 01_dc_contributor_author
                        item.clearMetadata(metadata[1], metadata[2],
                                metadata.length > 3 ? metadata[3] : null, Item.ANY);
    
                        List<String> sortedParamNames = sortParameters(request);
                        
                        for (String p : sortedParamNames)
                        {
                            // e.g. value_3_dc_contributor_author_00
                            if (p.startsWith("value_" + selectedId))
                            {
                                /*
                                 * It's a metadata value - it will be of the form
                                 * value_element_1 OR value_element_qualifier_2 (the
                                 * number being the sequence number) We use a
                                 * StringTokenizer to extract these values
                                 */
                                StringTokenizer st = new StringTokenizer(p, "_");
    
                                st.nextToken(); // Skip "value"
                                st.nextToken(); // Skip "id"
    
                                String schema = st.nextToken();
    
                                String element = st.nextToken();
    
                                String qualifier = null;
    
                                if (st.countTokens() == 2)
                                {
                                    qualifier = st.nextToken();
                                }
    
                                String[] checkTokenized = Utils.tokenize(fieldChoice
                                        .substring(fieldChoice.indexOf("_") + 1));
                                if (schema.equals(checkTokenized[0])
                                        && element.equals(checkTokenized[1])
                                        && (qualifier != null
                                                && checkTokenized.length == 3)
                                        && qualifier.equals(checkTokenized[2]))
                                {
                                    String sequenceNumber = st.nextToken();
    
                                    // Get a string with "element" for
                                    // unqualified or
                                    // "element_qualifier"
                                    String key = MetadataField.formKey(schema,
                                            element, qualifier);
    
                                    // Get the language
                                    String language = request
                                            .getParameter("language_" + fieldChoice
                                                    + "_" + sequenceNumber);
    
                                    // trim language and set empty
                                    // string language =
                                    // null
                                    if (StringUtils.isBlank(language))
                                    {
                                        language = null;
                                    }
    
                                    // Get the authority key if any
                                    String authority = request.getParameter(
                                            "choice_" + fieldChoice + "_authority_"
                                                    + sequenceNumber);
    
                                    // Get the authority confidence
                                    // value, passed as
                                    // symbolic name
                                    String sconfidence = request.getParameter(
                                            "choice_" + fieldChoice + "_confidence_"
                                                    + sequenceNumber);
                                    int confidence = (StringUtils
                                            .isBlank(sconfidence))
                                                    ? Choices.CF_UNSET
                                                    : Integer.parseInt(sconfidence);
    
                                    // Get the value
                                    String value = request.getParameter(p).trim();
                                    templateEmailParamFieldKey = key;
                                    templateEmailParamMetadataValue = value;
                                    templateEmailParamMetadataAuthority = authority;
                                    templateEmailParamMetadataConfidence = ""
                                            + confidence;
                                    if (StringUtils.isBlank(authority))
                                    {
                                        if (sequenceNumber.equals(sequenceChoice))
                                        {
                                            authority = crisID;
                                            if (selfClaim)
                                            {
                                                confidence = Choices.CF_ACCEPTED;
                                            }
                                            else
                                            {
                                                confidence = Choices.CF_UNCERTAIN;
                                                gotoReview = true;
                                            }
                                        }
                                        else
                                        {
                                            authority = null;
                                        }
                                    }
                                    else
                                    {
                                        if (authority.equals(crisID) && selfClaim)
                                        {
                                            confidence = Choices.CF_ACCEPTED;
                                        }
                                        else if (sequenceNumber
                                                .equals(sequenceChoice))
                                        {
                                            gotoReview = true;
                                        }
                                    }
    
                                    item.addMetadata(schema, element, qualifier,
                                            language, value, authority, confidence);
    
                                }
    
                            }
                        }
                        
                        //if need review by administrator write the metadata field with details
                        if(StringUtils.isBlank(templateEmailParamUserNote)) {
                            templateEmailParamUserNote = I18nUtil.getMessage(
                                    "jsp.dspace.authority-listclaim.default.note",
                                    context.getCurrentLocale(), false);
                        }
                        if (gotoReview)
                        {
                            item.addMetadata(METADATA_MESSAGE[0],
                                    METADATA_MESSAGE[1], METADATA_MESSAGE[2],
                                    Item.ANY,
                                    sdf.format(now) + "|||" + crisID + "|||"
                                            + submitMode + "|||" + fieldChoice.substring(
                                                    fieldChoice.indexOf("_") + 1)
                                            + "|||" + templateEmailParamUserNote);
                        }
    
                        item.update();
                        context.commit();
                    }
                    if (itemRejectedIDs.size() > 0)
                    {
                        discard(context, crisID, cam, fieldChoice, itemRejectedIDs, submitMode);
                    }
                }
    
                prepareEmail(context, handle, crisID, notifyGroupSelfClaim, submitMode, gotoReview,
                        templateEmailParamFieldKey,
                        templateEmailParamMetadataValue,
                        templateEmailParamMetadataAuthority,
                        templateEmailParamMetadataConfidence,
                        templateEmailParamUserNote, item, fieldChoice);
            }
        }

    }

    private void discard(Context context, String crisID, ChoiceAuthorityManager cam,
            String fieldChoice, Set<Integer> itemRejectedIDs, String submitMode) throws SQLException, AuthorizeException
    {
        // notify reject
        int[] ids = new int[itemRejectedIDs.size()];
        Iterator<Integer> iter = itemRejectedIDs.iterator();
        int i = 0;
        while (iter.hasNext())
        {
            ids[i] = (Integer) iter.next();
            i++;
        }
   
        String[] splitted = fieldChoice.split("_");
        // skip item id ---> e.g. 01_dc_contributor_author
        String schema = splitted[1];
        String element = splitted[2];
        String qualifier = (splitted.length == 4) ? splitted[3]
                : null;

        cam.notifyReject(ids, schema, element, qualifier, crisID);
    }

    private void findSelectedPersons(HttpServletRequest request, int selectedId,
            List<String> choices)
    {
        Enumeration e = request.getParameterNames();
        
        while (e.hasMoreElements())
        {
            String parameterName = (String) e.nextElement();

            // userchoice_<identifier>_schema_element_qualifier ->
            // userchoice_16_dc_contributor_author
            if (parameterName.startsWith("userchoice_" + selectedId))
            {
                // <sequencenumber>_<identifier>_schema_element_qualifier ->
                // 00_16_dc_contributor_author
                choices.add(request.getParameter(parameterName));
            }
        }
    }

    private void prepareEmail(Context context, String handle, String crisID,
            String notifyGroupSelfClaim, String submitMode,
            boolean isReview, String templateEmailParamFieldKey,
            String templateEmailParamMetadataValue,
            String templateEmailParamMetadataAuthority,
            String templateEmailParamMetadataConfidence,
            String templateEmailParamUserNote, Item item, String fieldChoice)
    {
        if (!SUBMIT_DISCARD.equalsIgnoreCase(submitMode))
        {
            if (isReview)
            {
                // if template email exist we have to send the follow
                // notification: review info for administrators; request done
                // for user
                sendEmail(context, PUBLICATION_CLAIM_REVIEW,
                        notifyGroupSelfClaim, null, templateEmailParamFieldKey,
                        templateEmailParamMetadataValue,
                        templateEmailParamMetadataAuthority,
                        templateEmailParamMetadataConfidence,
                        templateEmailParamUserNote, handle, crisID,
                        context.getCurrentUser(), item.getName());
                sendEmail(context, PUBLICATION_CLAIM_REQUEST, null,
                        context.getCurrentUser().getEmail(),
                        templateEmailParamFieldKey,
                        templateEmailParamMetadataValue,
                        templateEmailParamMetadataAuthority,
                        templateEmailParamMetadataConfidence,
                        templateEmailParamUserNote, handle, crisID,
                        context.getCurrentUser(), item.getName());
            }
            else
            {
                // automatically claim the publication, send notification to the
                // user and administrators
                sendEmail(context, PUBLICATION_CLAIMED, notifyGroupSelfClaim,
                        context.getCurrentUser().getEmail(),
                        templateEmailParamFieldKey,
                        templateEmailParamMetadataValue,
                        templateEmailParamMetadataAuthority,
                        templateEmailParamMetadataConfidence,
                        templateEmailParamUserNote, handle, crisID,
                        context.getCurrentUser(), item.getName());
            }
        }
    }

    private List<String> sortParameters(HttpServletRequest request)
    {
        Enumeration unsortedParamNames = request.getParameterNames();
        List<String> sortedParamNames = new LinkedList<String>();

        while (unsortedParamNames.hasMoreElements())
        {
            sortedParamNames
                    .add((String) unsortedParamNames.nextElement());
        }

        // Sort the list
        Collections.sort(sortedParamNames);
        return sortedParamNames;
    }

    private void sendEmail(Context context, String templateEmail,
            String groupName, String emailUser, String field, String value,
            String authority, String confidence, String note, String handle,
            String crisId, EPerson currentUser, String itemDCTitle)
    {

        org.dspace.core.Email email;
        try
        {
            email = org.dspace.core.Email.getEmail(I18nUtil.getEmailFilename(
                    context.getCurrentLocale(), templateEmail));

            if (StringUtils.isBlank(authority))
            {
                authority = context.getCrisID();
            }
            try
            {
                if (StringUtils.isNotBlank(emailUser))
                {
                    email.addRecipient(emailUser);
                
                    if (StringUtils.isNotBlank(groupName)) {
                        Group group = Group.findByName(context, groupName);
                        if (group != null && !group.isEmpty())
                        {
                            for (EPerson eperson : group.getMembers())
                            {
                                email.addRecipientCC(eperson.getEmail());
                            }
                        }
                        else
                        {
                            log.warn(
                                    "No get eperson from group (check notify-publication.claim.group.name configuration)");
                            return;
                        }
                    }
                }
                else
                {
                    Group group = Group.findByName(context, groupName);
                    if (group != null && !group.isEmpty())
                    {
                        for (EPerson eperson : group.getMembers())
                        {
                            email.addRecipient(eperson.getEmail());
                        }
                    }
                    else
                    {
                        log.warn(
                                "No get eperson from group (check notify-publication.claim.group.name configuration)");
                        return;
                    }
                }
                email.addArgument(field);
                email.addArgument(value);
                email.addArgument(authority);
                email.addArgument(confidence);
                email.addArgument(ConfigurationManager.getProperty("dspace.url")
                        + "/cris/rp/" + crisId);
                email.addArgument(ConfigurationManager.getProperty("dspace.url")
                        + "/handle/" + handle);
                email.addArgument(crisId);
                email.addArgument(note);
                email.addArgument(itemDCTitle);
                email.addArgument(currentUser.getFullName());
                email.addArgument(currentUser.getEmail());
                email.send();
            }
            catch (SQLException | MessagingException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        catch (

        IOException e)
        {
            log.error(e.getMessage(), e);
        }

    }
}
