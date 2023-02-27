import { Component, Input } from '@angular/core';

import { Item } from '../../../../../core/shared/item.model';
import { ItemPageFieldComponent } from '../item-page-field.component';

@Component({
  selector: 'ds-item-page-author-field',
  templateUrl: '../item-page-field.component.html'
})
/**
 * This component is used for displaying the author (dc.contributor.author, dc.creator and
 * dc.contributor) metadata of an item.
 *
 * Note that it purely deals with metadata. It won't turn related Person authors into links to their
 * item page. For that use a {@link MetadataRepresentationListComponent} instead.
 */
export class ItemPageAuthorFieldComponent extends ItemPageFieldComponent {

  /**
   * The item to display metadata for
   */
  @Input() item: Item;

  /**
   * Separator string between multiple values of the metadata fields defined
   * @type {string}
   */
  separator: string;

  /**
   * Fields (schema.element.qualifier) used to render their values.
   * In this component, we want to display values for metadata 'dc.contributor.author', 'dc.creator' and 'dc.contributor'
   */
  fields: string[] = [
    'dc.contributor.author',
    'dc.creator',
    'dc.contributor'
  ];

  /**
   * Label i18n key for the rendered metadata
   */
  label = 'item.page.author';

}
