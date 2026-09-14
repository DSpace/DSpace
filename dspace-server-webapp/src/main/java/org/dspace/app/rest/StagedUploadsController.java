/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.StagedUploadRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.StagedUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staging of large files in bounded, individually authenticated requests, for any authenticated user.
 * A staged upload is consumed later by a target endpoint, for example
 * {@code POST /api/system/scripts/{name}/processes} with a JSON body. Raw chunk bodies are read inside
 * the authorized method, so authentication happens before body consumption when proxies stream requests.
 */
@RestController
@RequestMapping(StagedUploadsController.PATH)
public class StagedUploadsController {
    public static final String PATH = "/api/" + RestModel.CORE + "/uploads";
    @Autowired
    private StagedUploadService uploads;
    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    /** Advertise the resource as the {@code uploads} link of the API root. */
    @PostConstruct
    public void registerEndpoint() {
        discoverableEndpointsService.register(this, List.of(Link.of(PATH, "uploads")));
    }

    /** Create an owner-bound upload after validating the complete file size; no data is received yet. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<EntityModel<StagedUploadRest>> create(@Valid @RequestBody CreateStagedUploadRest input,
                                                               HttpServletRequest request) throws Exception {
        StagedUploadRest upload = uploads.create(owner(request), input.name(), input.size());
        EntityModel<StagedUploadRest> resource = resource(upload);
        return ResponseEntity.created(resource.getRequiredLink("self").toUri()).body(resource);
    }

    /** Read committed progress, or discover the result of an earlier handoff. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public EntityModel<StagedUploadRest> get(@PathVariable UUID id, HttpServletRequest request) throws Exception {
        return resource(uploads.get(owner(request), id));
    }

    /** Commit a bounded chunk at a byte offset; identical retries are idempotent. */
    @PutMapping(value = "/{id}/chunks/{offset}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public EntityModel<StagedUploadRest> put(@PathVariable UUID id, @PathVariable long offset,
                                             HttpServletRequest request) throws Exception {
        UUID user = owner(request);
        return resource(uploads.put(user, id, offset, request.getInputStream()));
    }

    /** Cancel staging without affecting anything an earlier handoff created. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) throws Exception {
        uploads.delete(owner(request), id);
        return ResponseEntity.noContent().build();
    }

    private UUID owner(HttpServletRequest request) {
        return ContextUtil.obtainContext(request).getCurrentUser().getID();
    }

    private EntityModel<StagedUploadRest> resource(StagedUploadRest upload) {
        String self = linkTo(StagedUploadsController.class).slash(upload.id()).toString();
        EntityModel<StagedUploadRest> resource = EntityModel.of(upload,
            Link.of(self).withSelfRel(), Link.of(self + "/chunks/{offset}").withRel("chunks"));
        if (upload.result() != null) {
            resource.add(Link.of(upload.result()).withRel("result"));
        }
        return resource;
    }

    /** Upload metadata; filenames are also validated by the staging service. */
    public record CreateStagedUploadRest(@NotBlank @Size(max = 255) String name, @Positive long size) {
    }
}
