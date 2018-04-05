/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.util.Map;

import org.dspace.app.xmlui.aspect.administrative.SystemwideAlerts;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.TextArea;

/**
 * Control panel tab that handles system wide alerts.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 */
public class ControlPanelAlertsTab extends AbstractControlPanelTab {

	private static final Message T_alerts_head = message("xmlui.administrative.ControlPanel.alerts_head");
	private static final Message T_alerts_warning = message("xmlui.administrative.ControlPanel.alerts_warning");
	private static final Message T_alerts_message_label = message("xmlui.administrative.ControlPanel.alerts_message_label");
	private static final Message T_alerts_message_default = message("xmlui.administrative.ControlPanel.alerts_message_default");
	private static final Message T_alerts_countdown_label = message("xmlui.administrative.ControlPanel.alerts_countdown_label");
	private static final Message T_alerts_countdown_none = message("xmlui.administrative.ControlPanel.alerts_countdown_none");
	private static final Message T_alerts_countdown_5 = message("xmlui.administrative.ControlPanel.alerts_countdown_5");
	private static final Message T_alerts_countdown_15 = message("xmlui.administrative.ControlPanel.alerts_countdown_15");
	private static final Message T_alerts_countdown_30 = message("xmlui.administrative.ControlPanel.alerts_countdown_30");
	private static final Message T_alerts_countdown_60 = message("xmlui.administrative.ControlPanel.alerts_countdown_60");
	private static final Message T_alerts_countdown_keep = message("xmlui.administrative.ControlPanel.alerts_countdown_keep");
	private static final Message T_alerts_session_label = message("xmlui.administrative.ControlPanel.alerts_session_label");
	private static final Message T_alerts_session_all_sessions = message("xmlui.administrative.ControlPanel.alerts_session_all_sessions");
	private static final Message T_alerts_session_current_sessions = message("xmlui.administrative.ControlPanel.alerts_session_current_sessions");
	private static final Message T_alerts_session_only_administrative = message("xmlui.administrative.ControlPanel.alerts_session_only_administrative_sessions");
	private static final Message T_alerts_session_note = message("xmlui.administrative.ControlPanel.alerts_session_note");
	private static final Message T_alerts_submit_activate = message("xmlui.administrative.ControlPanel.alerts_submit_activate");
	private static final Message T_alerts_submit_deactivate = message("xmlui.administrative.ControlPanel.alerts_submit_deactivate");

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {
		List form = div.addList("system-wide-alerts", List.TYPE_FORM);
		form.setHead(T_alerts_head);

		form.addItem(T_alerts_warning);

		TextArea message = form.addItem().addTextArea("message");
		message.setAutofocus("autofocus");
		message.setLabel(T_alerts_message_label);
		message.setSize(5, 45);
		if (SystemwideAlerts.getMessage() == null) {
			message.setValue(T_alerts_message_default);
		} else {
			message.setValue(SystemwideAlerts.getMessage());
		}

		Select countdown = form.addItem().addSelect("countdown");
		countdown.setLabel(T_alerts_countdown_label);

		countdown.addOption(0, T_alerts_countdown_none);
		countdown.addOption(5, T_alerts_countdown_5);
		countdown.addOption(15, T_alerts_countdown_15);
		countdown.addOption(30, T_alerts_countdown_30);
		countdown.addOption(60, T_alerts_countdown_60);

		// Is there a current count down active?
		if (SystemwideAlerts.isAlertActive()
				&& SystemwideAlerts.getCountDownToo()
						- System.currentTimeMillis() > 0) {
			countdown.addOption(true, -1, T_alerts_countdown_keep);
		} else {
			countdown.setOptionSelected(0);
		}

		Select restrictsessions = form.addItem().addSelect("restrictsessions");
		restrictsessions.setLabel(T_alerts_session_label);
		restrictsessions.addOption(SystemwideAlerts.STATE_ALL_SESSIONS,
				T_alerts_session_all_sessions);
		restrictsessions.addOption(SystemwideAlerts.STATE_CURRENT_SESSIONS,
				T_alerts_session_current_sessions);
		restrictsessions.addOption(
				SystemwideAlerts.STATE_ONLY_ADMINISTRATIVE_SESSIONS,
				T_alerts_session_only_administrative);
		restrictsessions.setOptionSelected(SystemwideAlerts
				.getRestrictSessions());

		form.addItem(T_alerts_session_note);

		Item actions = form.addItem();
		actions.addButton("submit_activate").setValue(T_alerts_submit_activate);
		actions.addButton("submit_deactivate").setValue(
				T_alerts_submit_deactivate);
	}

}

