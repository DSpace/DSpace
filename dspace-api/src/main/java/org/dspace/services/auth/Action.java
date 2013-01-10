/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.auth;

/**
 * The action enum is intended to replace the use of some contants
 * defined in the org.dspace.core.Constants class. 
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public enum Action {
	NONE(-1),
	READ(0),
	WRITE(1),
	DELETE(2),
	ADD(3),
	REMOVE(4),
	WORKFLOW_STEP_1(5),
	WORKFLOW_STEP_2(6),
	WORKFLOW_STEP_3(7),
	WORKFLOW_ABORT(8),
	DEFAULT_BITSTREAM_READ(9),
	DEFAULT_ITEM_READ(10),
	ADMIN(11)
	;

	private final int id;

	Action(int id) { this.id = id; }
	
	public int getId () {
		return this.id;
	}
	
	public static Action getById(int id)
    {
		Action type = null;

        for (Action bonusTypeTemp : Action.values())
        {
            if(id == bonusTypeTemp.id)
            {
                type = bonusTypeTemp;
                break;
            }
        }

        return type;
    }
}
