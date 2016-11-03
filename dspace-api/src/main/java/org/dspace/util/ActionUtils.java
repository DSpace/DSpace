/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.dspace.core.I18nUtil;

public class ActionUtils
{
    
    public static final String ACTION_FAKE_WS = "fake_ws";
    public static final String ACTION_IGNORE_WS = "ignore_ws";
    public static final String ACTION_FAKE_WF1 = "fake_wf1";
    public static final String ACTION_FAKE_WF2 = "fake_wf2";
    public static final String ACTION_IGNORE_WF1 = "ignore_wf1";
    public static final String ACTION_IGNORE_WF2 = "ignore_wf2";

    public static Map<String, String> createActionsLabel(Locale locale)
    {
        Map<String, String> actionsLabel = new HashMap<String, String>();
        //dedup
        actionsLabel.put(ACTION_FAKE_WF1, I18nUtil.getMessage("jsp.dedup.table.actions.fakewf1", locale));
        actionsLabel.put(ACTION_FAKE_WF2, I18nUtil.getMessage("jsp.dedup.table.actions.fakewf2", locale));
        actionsLabel.put(ACTION_FAKE_WS, I18nUtil.getMessage("jsp.dedup.table.actions.fakews", locale));
        actionsLabel.put(ACTION_IGNORE_WF1, I18nUtil.getMessage("jsp.dedup.table.actions.ignorewf1", locale));
        actionsLabel.put(ACTION_IGNORE_WF2, I18nUtil.getMessage("jsp.dedup.table.actions.ignorewf2", locale));
        actionsLabel.put(ACTION_IGNORE_WS, I18nUtil.getMessage("jsp.dedup.table.actions.ignorews", locale));
        return actionsLabel;
    }
}
