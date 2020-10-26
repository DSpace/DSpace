/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.layout.service.InProgressSubmissionService;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class InProgressSubmissionServiceImpl implements InProgressSubmissionService {
    private final Cache<UUID, InProgressSubmission> cache;

    private final DCInputsReader dcInputsReader;

    /**
     * Default constructor
     *
     * @throws DCInputsReaderException in case of error during initialization of {@link DCInputsReader}
     */
    public InProgressSubmissionServiceImpl() throws DCInputsReaderException {
        this(new DCInputsReader());
    }

    /**
     * default scoped constructor, used for testing purposes
     *
     * @param cache          cache container
     * @param dcInputsReader custom implementation of {@link DCInputsReader}
     */
    InProgressSubmissionServiceImpl(final DCInputsReader dcInputsReader) {
        this.cache = CacheBuilder.newBuilder()
                                 .expireAfterWrite(10, TimeUnit.MINUTES)
                                 .build();
        this.dcInputsReader = dcInputsReader;
    }

    @Override
    public void add(final InProgressSubmission workspaceItem) {
        cache.put(workspaceItem.getItem().getID(), workspaceItem);
    }

    @Override
    public boolean hasSubmissionRights(final UUID itemId, final MetadataValue metadataValue) {

        return Optional.ofNullable(cache.getIfPresent(itemId))
                       .map(inProgressSubmission -> check(inProgressSubmission, metadataValue))
                       .orElse(false);
    }

    @Override
    public void remove(final UUID itemId) {
        cache.invalidate(itemId);
    }

    /**
     * checks if service internal list contains uuid
     *
     * @param uuid uuid to be checked
     * @return
     */
    boolean contains(final UUID uuid) {
        return Objects.nonNull(cache.getIfPresent(uuid));
    }

    private boolean check(final InProgressSubmission inProgressSubmission, final MetadataValue metadataValue) {
        try {
            return dcInputsReader.getInputsByCollection(inProgressSubmission.getCollection())
                                 .stream()
                                 .anyMatch(dc -> dc.isFieldPresent(metadataValue.getMetadataField().toString('.')));
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
