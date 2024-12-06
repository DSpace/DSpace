/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;
/**
 * REQUESTED means acknowledgements not received yet
 * ACCEPTED means acknowledgements of "Accept" type received
 * REJECTED means ack of "TentativeReject" type received
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 */
public enum NotifyRequestStatusEnum {
    REJECTED, ACCEPTED, REQUESTED
}
