/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

/**
 * Status of a staged upload. Received bytes are committed bytes, not progress of whatever consumes the file.
 * @param id upload identifier
 * @param name original file name
 * @param size total file length
 * @param chunkSize maximum bytes in one request
 * @param receivedBytes committed file bytes
 * @param state UPLOADING, CONSUMING, CONSUMED or FAILED
 * @param result link of the resource created from this upload, once its handoff recorded one
 */
public record StagedUploadRest(UUID id, String name, long size, int chunkSize, long receivedBytes,
                               String state, String result) {
}
