/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.time.LocalDate;

/**
 * This class represents a single log file line and the operations that can be
 * performed on it
 *
 * The components that it represents are: Date, Level, User, Action, and additional
 * Params
 *
 * @author Richard Jones
 */
public class LogLine {
    /**
     * the date of the log file line
     */
    private LocalDate date = null;

    /**
     * the level of the log line type
     */
    private String level = null;

    /**
     * the user performing the logged action
     */
    private String user = null;

    /**
     * the action being performed
     */
    private String action = null;

    /**
     * the parameters associated with the line
     */
    private String params = null;

    /**
     * constructor to create new statistic
     */
    LogLine(LocalDate date, String level, String user, String action, String params) {
        this.date = date;
        this.level = level;
        this.user = user;
        this.action = action;
        this.params = params;
    }

    /**
     * get the date of the log line
     *
     * @return the date of this log line
     */
    public LocalDate getDate() {
        return this.date;
    }


    /**
     * get the level of this log line
     *
     * @return the level of the log line
     */
    public String getLevel() {
        return this.level;
    }


    /**
     * get the user performing the logged action
     *
     * @return the user performing the logged action
     */
    public String getUser() {
        return this.user;
    }


    /**
     * get the action being performed
     *
     * @return the logged action
     */
    public String getAction() {
        return this.action;
    }


    /**
     * get the parameters associated with the action
     *
     * @return the parameters associated with the action
     */
    public String getParams() {
        return this.params;
    }


    /**
     * find out if this log file line is before the given date
     *
     * @param compareDate the date to be compared to
     * @return true if the line is before the given date, false if not
     */
    public boolean beforeDate(LocalDate compareDate) {
        if (compareDate != null) {
            return this.date.isBefore(compareDate);
        }
        return false;
    }


    /**
     * find out if this log file line is after the given date
     *
     * @param compareDate the date to be compared to
     * @return true if the line is after the given date, false if not
     */
    public boolean afterDate(LocalDate compareDate) {
        if (compareDate != null) {
            return this.date.isAfter(compareDate);
        }
        return false;
    }


    /**
     * find out if the log line is of the given level.  Levels are either
     * INFO, WARN or ERROR
     *
     * @param level the level we want to test for
     * @return true if the line is of the specified level, false if not
     */
    public boolean isLevel(String level) {
        if (this.getLevel().equals(level)) {
            return true;
        }
        return false;
    }


    /**
     * find out if the log line is of the given action.  Actions are not
     * directly constrained by the vocabulary, and any system module may define
     * any action name for its behaviour
     *
     * @param action the action we want to test for
     * @return true if the line is of the specified action, false if not
     */
    public boolean isAction(String action) {
        if (this.getAction().equals(action)) {
            return true;
        }
        return false;
    }

}
