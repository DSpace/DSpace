import { MetadataRepresentationListElementComponent } from '../metadata-representation-list-element.component';
import { Component, OnInit } from '@angular/core';
import { ItemMetadataRepresentation } from '../../../../core/shared/metadata-representation/item/item-metadata-representation.model';
import { getItemPageRoute } from '../../../../item-page/item-page-routing-paths';

@Component({
  selector: 'ds-item-metadata-representation-list-element',
  template: ''
})
/**
 * An abstract class for displaying a single ItemMetadataRepresentation
 */
export class ItemMetadataRepresentationListElementComponent extends MetadataRepresentationListElementComponent implements OnInit {
  metadataRepresentation: ItemMetadataRepresentation;

  /**
   * Route to the item's page
   */
  itemPageRoute: string;

  ngOnInit(): void {
    this.itemPageRoute = getItemPageRoute(this.metadataRepresentation);
  }
}
