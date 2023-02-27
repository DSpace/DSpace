import { Component, Input } from '@angular/core';
import { Item } from '../../../../core/shared/item.model';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { BrowseDefinition } from '../../../../core/shared/browse-definition.model';
import { BrowseDefinitionDataService } from '../../../../core/browse/browse-definition-data.service';
import { getRemoteDataPayload } from '../../../../core/shared/operators';

/**
 * This component can be used to represent metadata on a simple item page.
 * It expects one input parameter of type Item to which the metadata belongs.
 * This class can be extended to print certain metadata.
 */

@Component({
    templateUrl: './item-page-field.component.html'
})
export class ItemPageFieldComponent {

    constructor(protected browseDefinitionDataService: BrowseDefinitionDataService) {
    }

    /**
     * The item to display metadata for
     */
    @Input() item: Item;

    /**
     * Whether the {@link MarkdownPipe} should be used to render this metadata.
     */
    enableMarkdown = false;

    /**
     * Fields (schema.element.qualifier) used to render their values.
     */
    fields: string[];

    /**
     * Label i18n key for the rendered metadata
     */
    label: string;

    /**
     * Separator string between multiple values of the metadata fields defined
     * @type {string}
     */
    separator = '<br/>';

    /**
     * Whether any valid HTTP(S) URL should be rendered as a link
     */
    urlRegex?: string;

    /**
     * Return browse definition that matches any field used in this component if it is configured as a browse
     * link in dspace.cfg (webui.browse.link.<n>)
     */
    get browseDefinition(): Observable<BrowseDefinition> {
      return this.browseDefinitionDataService.findByFields(this.fields).pipe(
        getRemoteDataPayload(),
        map((def) => def)
      );
    }
}
