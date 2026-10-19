/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

/**
 * Enum representing the various types of details that can be associated with an event.
 * Each constant in this enum corresponds to a specific type of detail that can be used
 * to describe or categorize an event in the system.
 *
 * <p>Available detail types:
 * <ul>
 *   <li>{@code DESCRIPTION}: Represents a description of the event.</li>
 *   <li>{@code HANDLE}: Represents a handle associated with the event.</li>
 *   <li>{@code DSO_TYPE}: Represents the type of the DSpace object involved in the event.</li>
 *   <li>{@code BITSTREAM_SEQUENCE_ID}: Represents the sequence ID of a bitstream.</li>
 *   <li>{@code DSO_SUMMARY}: Represents a summary of the DSpace object.</li>
 *   <li>{@code DSO_NAME}: Represents the name of the DSpace object.</li>
 *   <li>{@code ACTION}: Represents the action performed during the event.</li>
 *   <li>{@code EPERSON_EMAIL}: Represents the email of the EPerson associated with the event.</li>
 *   <li>{@code INFO}: Represents additional information about the event.</li>
 * </ul>
 *
 * <p>This enum is used in conjunction with the {@link EventDetail} class to provide
 * structured and meaningful details about events in the system.
 *
 * <p>Example usage:
 * <pre>
 *     DetailType detailType = DetailType.DESCRIPTION;
 * </pre>
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public enum DetailType {
    DESCRIPTION, HANDLE, DSO_TYPE, BITSTREAM_SEQUENCE_ID, DSO_SUMMARY, DSO_NAME, ACTION, EPERSON_EMAIL,
    BITSTREAM_CHECKSUM, INFO
}
