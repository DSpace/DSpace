/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.orm.entity.Eperson;
import org.dspace.orm.entity.IDSpaceObject;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class AuthorizationException extends Exception {
	private static final long serialVersionUID = -6001782807840859656L;
	private List<Action> actions;
	private Eperson eperson;
	private IDSpaceObject object;
	
	public AuthorizationException(Action action, Eperson eperson, IDSpaceObject object) {
		super(
				((eperson != null) ? "User "+eperson.getEmail() : "") +
				((object != null) ? "Unable to access object "+object.getClass().getSimpleName()+" with id "+object.getID() : "") +
				" for action = "+action.name()
				);
		this.actions = new ArrayList<Action>();
		this.actions.add(action);
		this.eperson = eperson;
		this.object = object;
	}
	
	private static String join (Action[] values) {
		List<String> ints = new ArrayList<String>();
		for (Action i : values)
			ints.add(i.name());
		return StringUtils.join(ints, ", ");
	}

	public AuthorizationException(Action[] action, Eperson eperson, IDSpaceObject object) {
		super(
				((eperson != null) ? "User "+eperson.getEmail() +" ": "") +
				((object != null) ? "Unable to access object "+object.getClass().getSimpleName()+" with id "+object.getID() : "") +
				((action.length > 0) ? " for actions = "+join(action) : "")
				);
		this.actions = new ArrayList<Action>();
		for (Action i : action)
			this.actions.add(i);
		this.eperson = eperson;
		this.object = object;
	}
	
	public List<Action> getActions() {
		return actions;
	}
	public Eperson getEperson() {
		return eperson;
	}
	public IDSpaceObject getObject() {
		return object;
	}
	
	
	public boolean hasActions () {
		return (!this.getActions().isEmpty());
	}

	public boolean hasEperson () {
		return (this.eperson != null);
	}
	
	public boolean hasObject () {
		return (this.object != null);
	}
}
