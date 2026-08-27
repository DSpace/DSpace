/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import javax.servlet.http.HttpServletRequest;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Event;

// UM Changes
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.apache.logging.log4j.Logger;

/**
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public class UsageEvent extends Event {


    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(UsageEvent.class);

    public static enum Action {
        VIEW("view"),
        CREATE("create"),
        UPDATE("update"),
        DELETE("delete"),
        ADD("add"),
        REMOVE("remove"),
        BROWSE("browse"),
        SEARCH("search"),
        WORKFLOW("workflow"),
        LOGIN("login"),
        SUBSCRIBE("subscribe"),
        UNSUBSCRIBE("unsubscribe"),
        WITHDRAW("withdraw"),
        REINSTATE("reinstate");

        private final String text;

        Action(String text) {
            this.text = text;
        }

        String text() {
            return text;
        }
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private HttpServletRequest request;

    private String ip;

    private String userAgent;

    private String xforwardedfor;

    private Context context;

    private DSpaceObject object;

    private Action action;

    private static Boolean useProxies;

    private String referrer;

    private static String checkParams(Action action, HttpServletRequest request, Context context, DSpaceObject object) {
        StringBuilder eventName = new StringBuilder();
        if (action == null) {
            throw new IllegalStateException("action cannot be null");
        }

        if (action != Action.WORKFLOW && request == null) {
            throw new IllegalStateException("request cannot be null");
        }


        if (context == null) {
            throw new IllegalStateException("context cannot be null");
        }

        if (action != Action.WORKFLOW && action != Action.SEARCH && object == null) {
            throw new IllegalStateException("object cannot be null");
        } else if (object != null) {
            String objText = Constants.typeText[object.getType()].toLowerCase();
            eventName.append(objText).append(":");
        }
        eventName.append(action.text());

        return eventName.toString();
    }

    private static String checkParams(Action action, Context context, DSpaceObject object) {
        StringBuilder eventName = new StringBuilder();
        if (action == null) {
            throw new IllegalStateException("action cannot be null");
        }

//      if(action != Action.WORKFLOW)
//        {
//            throw new IllegalStateException("request cannot be null");
//        }


        if (context == null) {
            throw new IllegalStateException("context cannot be null");
        }

        if (action != Action.WORKFLOW && action != Action.SEARCH && object == null) {
            throw new IllegalStateException("object cannot be null");
        } else if (object != null) {
            String objText = Constants.typeText[object.getType()].toLowerCase();
            eventName.append(objText).append(":");
        }
        eventName.append(action.text());


        return eventName.toString();
    }

    // This one is used by view_bitstream_details
    public Context getContextSpecial() {

        // HttpServletRequest request = null;
        // RequestService requestService = new DSpace().getRequestService();

        // Request currentRequest = requestService.getCurrentRequest();
        // if ( currentRequest != null)
        // {
        //   request = currentRequest.getHttpServletRequest();
        // }

        // Set the session ID
        context.setExtraLogInfo("session_id="
                + request.getSession().getId());

        // This is how I get the true IP
        String ip = getRequest().getRemoteAddr();
        String referer = this.referrer;

        // String ip = request.getHeader("X-Forwarded-For");
        // if (useProxies == null) {
        //     useProxies = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("useProxies", false);
        // }
        // if(useProxies && request.getHeader("X-Forwarded-For") != null)
        // {
        //     /* This header is a comma delimited list */
        //         for(String xfip : request.getHeader("X-Forwarded-For").split(","))
        //     {
        //         if(!request.getHeader("X-Forwarded-For").contains(ip))
        //         {
        //             ip = xfip.trim();
        //         }
        //     }
        // }

        context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip + ":referer=" + referer);

        // Store the context in the request
        //request.setAttribute(DSPACE_CONTEXT, context);

        return context;
    }

    // This one is used by view_item_details
    // For item you want to indicate INSIDE/OUTSIDE status.
    public Context getContextSpecialItem() {

        // HttpServletRequest request = null;

        // RequestService requestService = new DSpace().getRequestService();

        // Request currentRequest = requestService.getCurrentRequest();
        // if ( currentRequest != null)
        // {
        //   log.info("REFITEM: currentRequest is null");  
        //   request = currentRequest.getHttpServletRequest();
        // }

        // This is how I get the true IP
        String ip = getRequest().getRemoteAddr();
        String referer = this.referrer;

        if ( ( referer == null ) || ( referer.isEmpty() ) )
            {
                referer = "null";
            }

        // Set the session ID
        context.setExtraLogInfo("session_id="
            + getRequest().getSession().getId());

        // UM Change
        // This did not get me the ip address.  I think this is the old way of doing it.
        // String ip = request.getHeader("X-Forwarded-For");
        // if (useProxies == null) {
        //     useProxies = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("useProxies", false);
        // }
        // if(useProxies && request.getHeader("X-Forwarded-For") != null)
        // {
        //     /* This header is a comma delimited list */
        //         for(String xfip : request.getHeader("X-Forwarded-For").split(","))
        //         {
        //             if(!request.getHeader("X-Forwarded-For").contains(ip))
        //             {
        //                 ip = xfip.trim();
        //             }
        //         }
        // }
        // End UM Change

        String InsideOutside;
        if ( referer != null )
        {
            Boolean InDeepBlue = referer.indexOf("deepblue") > 0;   
            if ( InDeepBlue )
            {
                InsideOutside = "INSIDE";
            }
            else
            {
                InsideOutside = "OUTSIDE";
            }
        }
        else
        {
            InsideOutside = "OUTSIDE";
        }
        context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip + ":referer=" + referer + ":collection=" + InsideOutside);

        // Store the context in the request
        //request.setAttribute(DSPACE_CONTEXT, context);

        return context;
    }

    public UsageEvent(Action action, HttpServletRequest request, Context context, DSpaceObject object) {

        super(checkParams(action, request, context, object));

        this.action = action;

        this.setResourceReference(
            object != null ? Constants.typeText[object.getType()].toLowerCase() + ":" + object.getID() : null);

        switch (action) {
            case CREATE:
            case UPDATE:
            case DELETE:
            case WITHDRAW:
            case REINSTATE:
            case ADD:
            case REMOVE:
                this.setModify(true);
                break;
            default:
                this.setModify(false);
        }

        if (context != null && context.getCurrentUser() != null) {
            this.setUserId(
                String.valueOf(context.getCurrentUser().getID()));
        }
        this.request = request;
        this.context = context;
        this.object = object;
    }

    public UsageEvent(Action action, String ip, String userAgent, String xforwardedfor, Context context,
                      DSpaceObject object) {

        super(checkParams(action, context, object));

        this.action = action;

        this.setResourceReference(
            object != null ? Constants.typeText[object.getType()].toLowerCase() + ":" + object.getID() : null);

        switch (action) {
            case CREATE:
            case UPDATE:
            case DELETE:
            case WITHDRAW:
            case REINSTATE:
            case ADD:
            case REMOVE:
                this.setModify(true);
                break;
            default:
                this.setModify(false);
        }

        if (context != null && context.getCurrentUser() != null) {
            this.setUserId(
                String.valueOf(context.getCurrentUser().getID()));
        }
        this.request = null;
        this.ip = ip;
        this.userAgent = userAgent;
        this.xforwardedfor = xforwardedfor;
        this.context = context;
        this.object = object;
    }

    public UsageEvent(Action action, HttpServletRequest request, Context context, DSpaceObject object,
                      String referrer) {
        this(action, request, context, object);
        setReferrer(referrer);
    }


    public HttpServletRequest getRequest() {
        return request;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getXforwardedfor() {
        return xforwardedfor;
    }

    public void setXforwardedfor(String xforwardedfor) {
        this.xforwardedfor = xforwardedfor;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public DSpaceObject getObject() {
        return object;
    }

    public void setObject(DSpaceObject object) {
        this.object = object;
    }

    public Action getAction() {
        return this.action;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}
