/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.model;

import org.dspace.app.bulkaccesscontrol.BulkAccessControl;

/**
 * Class that model the content of the JSON file used as input for the {@link BulkAccessControl}
 *
 * <code> <br/>
 * { <br/>
 *     item: { <br/>
 *        mode: "replace", <br/>
 *        accessConditions: [ <br/>
 *            { <br/>
 *              "name": "openaccess" <br/>
 *            } <br/>
 *        ] <br/>
 *     }, <br/>
 *     bitstream: { <br/>
 *       constraints: { <br/>
 *           uuid: [bit-uuid1, bit-uuid2, ..., bit-uuidN], <br/>
 *       }, <br/>
 *       mode: "add", <br/>
 *       accessConditions: [ <br/>
 *         { <br/>
 *          "name": "embargo", <br/>
 *          "startDate": "2024-06-24T23:59:59.999+0000" <br/>
 *         } <br/>
 *       ] <br/>
 *    } <br/>
 * }
 * </code>
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessControlInput {

    AccessConditionItem item;

    AccessConditionBitstream bitstream;

    public BulkAccessControlInput() {
    }

    public BulkAccessControlInput(AccessConditionItem item,
                                  AccessConditionBitstream bitstream) {
        this.item = item;
        this.bitstream = bitstream;
    }

    public AccessConditionItem getItem() {
        return item;
    }

    public void setItem(AccessConditionItem item) {
        this.item = item;
    }

    public AccessConditionBitstream getBitstream() {
        return bitstream;
    }

    public void setBitstream(AccessConditionBitstream bitstream) {
        this.bitstream = bitstream;
    }
}
