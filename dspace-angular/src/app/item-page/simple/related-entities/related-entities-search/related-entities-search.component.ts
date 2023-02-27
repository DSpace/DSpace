import { Component, Input, OnInit } from '@angular/core';
import { Item } from '../../../../core/shared/item.model';
import { isNotEmpty } from '../../../../shared/empty.util';
import { getFilterByRelation } from '../../../../shared/utils/relation-query.utils';

@Component({
  selector: 'ds-related-entities-search',
  templateUrl: './related-entities-search.component.html'
})
/**
 * A component to show related items as search results.
 * Related items can be faceted, or queried using an
 * optional search box.
 */
export class RelatedEntitiesSearchComponent implements OnInit {

  /**
   * The type of relationship to fetch items for
   * e.g. 'isAuthorOfPublication'
   */
  @Input() relationType: string;

  /**
   * An optional configuration to use for the search options
   */
  @Input() configuration: string;

  /**
   * The item to render relationships for
   */
  @Input() item: Item;

  /**
   * Whether or not the search bar and title should be displayed (defaults to true)
   * @type {boolean}
   */
  @Input() searchEnabled = true;

  /**
   * The ratio of the sidebar's width compared to the search results (1-12) (defaults to 4)
   * @type {number}
   */
  @Input() sideBarWidth = 4;

  fixedFilter: string;

  ngOnInit(): void {
    if (isNotEmpty(this.relationType) && isNotEmpty(this.item)) {
      this.fixedFilter = getFilterByRelation(this.relationType, this.item.id);
    }
  }

}
