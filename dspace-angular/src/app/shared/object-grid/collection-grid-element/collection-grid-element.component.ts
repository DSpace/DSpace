import { Component, Input } from '@angular/core';
import { Collection } from '../../../core/shared/collection.model';
import { AbstractListableElementComponent } from '../../object-collection/shared/object-collection-element/abstract-listable-element.component';
import { ViewMode } from '../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../object-collection/shared/listable-object/listable-object.decorator';
import { hasNoValue, hasValue } from '../../empty.util';
import { followLink } from '../../utils/follow-link-config.model';
import { LinkService } from '../../../core/cache/builders/link.service';

/**
 * Component representing a grid element for collection
 */
@Component({
  selector: 'ds-collection-grid-element',
  styleUrls: ['./collection-grid-element.component.scss'],
  templateUrl: './collection-grid-element.component.html',
})
@listableObjectComponent(Collection, ViewMode.GridElement)
export class CollectionGridElementComponent extends AbstractListableElementComponent<
  Collection
> {
  private _object: Collection;

  constructor(private linkService: LinkService) {
    super();
  }

  // @ts-ignore
  @Input() set object(object: Collection) {
    this._object = object;
    if (hasValue(this._object) && hasNoValue(this._object.logo)) {
      this.linkService.resolveLink<Collection>(
        this._object,
        followLink('logo')
      );
    }
  }

  get object(): Collection {
    return this._object;
  }
}
