import {Component} from '@angular/core';

import {Collection} from '../../../../../../app/core/shared/collection.model';
import {
  CollectionListElementComponent as BaseComponent
} from '../../../../../../app/shared/object-list/collection-list-element/collection-list-element.component';
import {ViewMode} from '../../../../../../app/core/shared/view-mode.model';
import {
  listableObjectComponent
} from '../../../../../../app/shared/object-collection/shared/listable-object/listable-object.decorator';
import {Context} from '../../../../../../app/core/shared/context.model';

@listableObjectComponent(Collection, ViewMode.ListElement, Context.Any, 'custom')

@Component({
  selector: 'ds-collection-list-element',
  // styleUrls: ['./collection-list-element.component.scss'],
  styleUrls: ['../../../../../../app/shared/object-list/collection-list-element/collection-list-element.component.scss'],
  // templateUrl: './collection-list-element.component.html'
  templateUrl: '../../../../../../app/shared/object-list/collection-list-element/collection-list-element.component.html'
})
/**
 * Component representing list element for a collection
 */
export class CollectionListElementComponent extends BaseComponent {}


