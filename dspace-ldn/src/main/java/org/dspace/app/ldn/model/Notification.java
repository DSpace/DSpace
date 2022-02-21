/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(value = {
    "@context",
    "id",
    "type",
    "actor",
    "context",
    "object",
    "origin",
    "target",
    "inReplyTo"
})
public class Notification extends Base {

    @JsonProperty("@context")
    private String[] c = new String[] {
        "https://www.w3.org/ns/activitystreams",
        "https://purl.org/coar/notify"
    };

    @JsonProperty("actor")
    private Actor actor;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("object")
    private Object object;

    @JsonProperty("origin")
    private Service origin;

    @JsonProperty("target")
    private Service target;

    @JsonProperty("inReplyTo")
    private String inReplyTo;

}
