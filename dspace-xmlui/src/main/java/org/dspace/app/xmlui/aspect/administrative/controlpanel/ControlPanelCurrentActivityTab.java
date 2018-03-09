/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.CurrentActivityAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * Control panel tab that displays current activity.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 *
 */
public class ControlPanelCurrentActivityTab extends AbstractControlPanelTab {

	private static final Message T_activity_head = message("xmlui.administrative.ControlPanel.activity_head");
	private static final Message T_stop_anonymous = message("xmlui.administrative.ControlPanel.stop_anonymous");
	private static final Message T_start_anonymous = message("xmlui.administrative.ControlPanel.start_anonymous");
	private static final Message T_stop_bot = message("xmlui.administrative.ControlPanel.stop_bot");
	private static final Message T_start_bot = message("xmlui.administrative.ControlPanel.start_bot");
	private static final Message T_activity_sort_time = message("xmlui.administrative.ControlPanel.activity_sort_time");
	private static final Message T_activity_sort_user = message("xmlui.administrative.ControlPanel.activity_sort_user");
	private static final Message T_activity_sort_ip = message("xmlui.administrative.ControlPanel.activity_sort_ip");
	private static final Message T_activity_sort_url = message("xmlui.administrative.ControlPanel.activity_sort_url");
	private static final Message T_activity_sort_agent = message("xmlui.administrative.ControlPanel.activity_sort_Agent");
	private static final Message T_activity_anonymous = message("xmlui.administrative.ControlPanel.activity_anonymous");
	private static final Message T_activity_none = message("xmlui.administrative.ControlPanel.activity_none");
	private static final Message T_seconds = message("xmlui.administrative.ControlPanel.seconds");
	private static final Message T_hours = message("xmlui.administrative.ControlPanel.hours");
	private static final Message T_minutes = message("xmlui.administrative.ControlPanel.minutes");
	private static final Message T_detail = message("xmlui.administrative.ControlPanel.detail");
	private static final Message T_show_hide = message("xmlui.administrative.ControlPanel.show_hide");
	private static final Message T_host = message("xmlui.administrative.ControlPanel.host");
	private static final Message T_puser = message("xmlui.administrative.ControlPanel.puser");
	private static final Message T_headers = message("xmlui.administrative.ControlPanel.headers");
	private static final Message T_cookies = message("xmlui.administrative.ControlPanel.cookies");

	protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

	@Override
	public void addBody(Map objectModel, Division div) throws WingException,
			SQLException {
		// 0) Update recording settings
		Request request = ObjectModelHelper.getRequest(objectModel);

		// Toggle anonymous recording
		String recordAnonymousString = request.getParameter("recordanonymous");
		if (recordAnonymousString != null) {
			if ("ON".equals(recordAnonymousString)) {
				CurrentActivityAction.setRecordAnonymousEvents(true);
			}
			if ("OFF".equals(recordAnonymousString)) {
				CurrentActivityAction.setRecordAnonymousEvents(false);
			}
		}

		// Toggle bot recording
		String recordBotString = request.getParameter("recordbots");
		if (recordBotString != null) {
			if ("ON".equals(recordBotString)) {
				CurrentActivityAction.setRecordBotEvents(true);
			}
			if ("OFF".equals(recordBotString)) {
				CurrentActivityAction.setRecordBotEvents(false);
			}
		}

		// 1) Determine how to sort
		EventSort sortBy = EventSort.TIME;
		String sortByString = request.getParameter("sortBy");
		if (EventSort.TIME.toString().equals(sortByString)) {
			sortBy = EventSort.TIME;
		}
		if (EventSort.URL.toString().equals(sortByString)) {
			sortBy = EventSort.URL;
		}
		if (EventSort.SESSION.toString().equals(sortByString)) {
			sortBy = EventSort.SESSION;
		}
		if (EventSort.AGENT.toString().equals(sortByString)) {
			sortBy = EventSort.AGENT;
		}
		if (EventSort.IP.toString().equals(sortByString)) {
			sortBy = EventSort.IP;
		}

		// 2) Sort the events by the requested sorting parameter
		java.util.List<CurrentActivityAction.Event> events = CurrentActivityAction
				.getEvents();
		Collections.sort(events, new ActivitySort<CurrentActivityAction.Event>(
				sortBy));
		Collections.reverse(events);
		
		div = div.addDivision("activitydiv", "well well-light"); 

		// 3) Toggle controls for anonymous and bot activity
		if (CurrentActivityAction.getRecordAnonymousEvents()) {
			div.addPara().addXref(web_link + "&sortBy=" + sortBy + "&recordanonymous=OFF").addContent(T_stop_anonymous);
		} else {
			div.addPara().addXref(web_link + "&sortBy=" + sortBy + "&recordanonymous=ON").addContent(T_start_anonymous);
		}

		if (CurrentActivityAction.getRecordBotEvents()) {
			div.addPara().addXref(web_link + "&sortBy=" + sortBy + "&recordbots=OFF").addContent(T_stop_bot);
		} else {
			div.addPara().addXref(web_link + "&sortBy=" + sortBy + "&recordbots=ON").addContent(T_start_bot);
		}

		// 4) Display the results Table
		// TABLE: activeUsers
		Table activeUsers = div.addTable("users", 1, 1);
		activeUsers.setHead(T_activity_head
				.parameterize(CurrentActivityAction.MAX_EVENTS));
		Row row = activeUsers.addRow(null, Row.ROLE_HEADER, "font_smaller");
		if (sortBy == EventSort.TIME) {
			row.addCell().addHighlight("bold").addXref(web_link + "&sortBy=" + EventSort.TIME).addContent(T_activity_sort_time);
		} else {
			row.addCell().addXref(web_link + "&sortBy=" + EventSort.TIME).addContent(T_activity_sort_time);
		}

		if (sortBy == EventSort.SESSION) {
			row.addCell().addHighlight("bold").addXref(web_link + "&sortBy=" + EventSort.SESSION).addContent(T_activity_sort_user);
		} else {
			row.addCell().addXref(web_link + "&sortBy=" + EventSort.SESSION).addContent(T_activity_sort_user);
		}

		if (sortBy == EventSort.IP) {
			row.addCell().addHighlight("bold").addXref(web_link + "&sortBy=" + EventSort.IP).addContent(T_activity_sort_ip);
		} else {
			row.addCell().addXref(web_link + "&sortBy=" + EventSort.IP).addContent(T_activity_sort_ip);
		}

		if (sortBy == EventSort.URL) {
			row.addCell().addHighlight("bold").addXref(web_link + "&sortBy=" + EventSort.URL).addContent(T_activity_sort_url);
		} else {
			row.addCell().addXref(web_link + "&sortBy=" + EventSort.URL).addContent(T_activity_sort_url);
		}

		if (sortBy == EventSort.AGENT) {
			row.addCell().addHighlight("bold").addXref(web_link + "&sortBy=" + EventSort.AGENT).addContent(T_activity_sort_agent);
		} else {
			row.addCell().addXref(web_link + "&sortBy=" + EventSort.AGENT).addContent(T_activity_sort_agent);
		}

		// add +
		row.addCellContent(T_detail);

		// Keep track of how many individual anonymous users there are, each
		// unique anonymous
		// user is assigned an index based upon the servlet session id.
		HashMap<String, Integer> anonymousHash = new HashMap<String, Integer>();
		int anonymousCount = 1;

		int shown = 0;
		for (CurrentActivityAction.Event event : events) {
			if (event == null) {
				continue;
			}

			shown++;

			Message timeStampMessage = null;
			long ago = System.currentTimeMillis() - event.getTimeStamp();

			if (ago > 2 * 60 * 60 * 1000) {
				timeStampMessage = T_hours
						.parameterize((ago / (60 * 60 * 1000)));
			} else if (ago > 60 * 1000) {
				timeStampMessage = T_minutes.parameterize((ago / (60 * 1000)));
			} else {
				timeStampMessage = T_seconds.parameterize((ago / (1000)));
			}

			Row eventRow = activeUsers.addRow(null, Row.ROLE_DATA, "font_smaller");

			eventRow.addCellContent(timeStampMessage);
			UUID eid = event.getEPersonID();
			EPerson eperson = ePersonService.find(context, eid);
			if (eperson != null) {
				String name = eperson.getFullName();
				eventRow.addCellContent(name);
			} else {
				// Is this a new anonymous user?
				if (!anonymousHash.containsKey(event.getSessionID())) {
					anonymousHash.put(event.getSessionID(), anonymousCount++);
				}

				eventRow.addCellContent(T_activity_anonymous
						.parameterize(anonymousHash.get(event.getSessionID())));
			}
			eventRow.addCellContent(event.getIP());
			eventRow.addCell().addXref(contextPath + "/" + event.getURL())
					.addContent("/" + event.getURL());
			eventRow.addCellContent(event.getDectectedBrowser());
			eventRow.addCell(null, null,
					"toggle-onclick-parent-next4 bold btn-link")
					.addContent(T_show_hide);
			final String not_present = "not present";

			String host = event.host != null ? (String) (event.host)
					: not_present;
			activeUsers.addRow(null, null, "hidden font_smaller").addCell(1, 6)
					.addContent(T_host.parameterize(host));

			String puser = event.puser != null ? event.puser : not_present;
			activeUsers.addRow(null, null, "hidden font_smaller").addCell(1, 6)
					.addContent(T_puser.parameterize(puser));

			//
			String headers = "";
			for (Map.Entry<String, String> o : event.headers.entrySet())
				headers += o.getKey() + ":[" + o.getValue() + "];";
			headers = headers != "" ? headers : not_present;
			activeUsers.addRow(null, null, "hidden").addCell(1, 6)
					.addContent(T_headers.parameterize(headers));

			//
			String cookies = "";
			for (Map.Entry<String, String> o : event.cookieMap.entrySet())
				cookies += o.getKey() + ":[" + o.getValue() + "];";
			cookies = cookies != "" ? cookies : not_present;
			activeUsers.addRow(null, null, "hidden font_smaller").addCell(1, 6)
					.addContent(T_cookies.parameterize(cookies));
		}

		if (shown == 0) {
			activeUsers.addRow().addCell(1, 5).addContent(T_activity_none);
		}
	}

	/** The possible sorting parameters */
	private static enum EventSort {
		TIME, URL, SESSION, AGENT, IP
	};

	/**
	 * Comparator to sort activity events by their access times.
	 */
	public static class ActivitySort<E extends CurrentActivityAction.Event>
			implements Comparator<E>, Serializable {
		// Sort parameter
		private EventSort sortBy;

		public ActivitySort(EventSort sortBy) {
			this.sortBy = sortBy;
		}

		/**
		 * Compare these two activity events based upon the given sort
		 * parameter. In the case of a tie, allways fallback to sorting based
		 * upon the timestamp.
		 */
		@Override
		public int compare(E a, E b) {
			// Protect against null events while sorting
			if (a != null && b == null) {
				return 1; // A > B
			} else if (a == null && b != null) {
				return -1; // B > A
			} else if (a == null && b == null) {
				return 0; // A == B
			}

			// Sort by the given ordering matrix
			if (EventSort.URL == sortBy) {
				String aURL = a.getURL();
				String bURL = b.getURL();
				int cmp = aURL.compareTo(bURL);
				if (cmp != 0) {
					return cmp;
				}
			} else if (EventSort.AGENT == sortBy) {
				String aAgent = a.getDectectedBrowser();
				String bAgent = b.getDectectedBrowser();
				int cmp = aAgent.compareTo(bAgent);
				if (cmp != 0) {
					return cmp;
				}
			} else if (EventSort.IP == sortBy) {
				String aIP = a.getIP();
				String bIP = b.getIP();
				int cmp = aIP.compareTo(bIP);
				if (cmp != 0) {
					return cmp;
				}

			} else if (EventSort.SESSION == sortBy) {
				// Ensure that all sessions with an EPersonID associated are
				// ordered to the top. Otherwise fall back to comparing session
				// IDs. Unfortunately we can not compare eperson names because
				// we do not have access to a context object.
				if (a.getEPersonID() != null && b.getEPersonID() == null) {
					return 1; // A > B
				} else if (a.getEPersonID() == null && b.getEPersonID() != null) {
					return -1; // B > A
				}

				String aSession = a.getSessionID();
				String bSession = b.getSessionID();
				int cmp = aSession.compareTo(bSession);
				if (cmp != 0) {
					return cmp;
				}
			}

			// All ways fall back to sorting by time, when events are equal.
			if (a.getTimeStamp() > b.getTimeStamp()) {
				return 1; // A > B
			} else if (a.getTimeStamp() > b.getTimeStamp()) {
				return -1; // B > A
			}
			return 0; // A == B
		}
	}

}

