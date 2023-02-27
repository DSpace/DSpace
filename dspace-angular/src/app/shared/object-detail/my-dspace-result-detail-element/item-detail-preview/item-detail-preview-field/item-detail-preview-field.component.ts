import { Component, Input } from '@angular/core';

import { Metadata } from '../../../../../core/shared/metadata.utils';
import { Item } from '../../../../../core/shared/item.model';
import { SearchResult } from '../../../../search/models/search-result.model';

/**
 * This component show values for the given item metadata
 */
@Component({
  selector: 'ds-item-detail-preview-field',
  templateUrl: './item-detail-preview-field.component.html'
})
export class ItemDetailPreviewFieldComponent {

  /**
   * The item to display
   */
  @Input() item: Item;

  /**
   * The search result object
   */
  @Input() object: SearchResult<any>;

  /**
   * The metadata label
   */
  @Input() label: string;

  /**
   * The metadata to show
   */
  @Input() metadata: string | string[];

  /**
   * The placeholder if there are no value to show
   */
  @Input() placeholder: string;

  /**
   * The value's separator
   */
  @Input() separator: string;

  /**
   * Gets all matching metadata string values from hitHighlights or dso metadata, preferring hitHighlights.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @returns {string[]} the matching string values or an empty array.
   */
  allMetadataValues(keyOrKeys: string | string[]): string[] {
    return Metadata.allValues([this.object.hitHighlights, this.item.metadata], keyOrKeys);
  }
}
