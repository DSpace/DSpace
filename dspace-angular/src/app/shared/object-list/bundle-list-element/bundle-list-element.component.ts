import { AbstractListableElementComponent } from '../../object-collection/shared/object-collection-element/abstract-listable-element.component';
import { Bundle } from '../../../core/shared/bundle.model';
import { Component } from '@angular/core';
import { listableObjectComponent } from '../../object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../core/shared/view-mode.model';

@Component({
  selector: 'ds-bundle-list-element',
  templateUrl: './bundle-list-element.component.html'
})
/**
 * This component is automatically used to create a list view for Bundle objects
 */
@listableObjectComponent(Bundle, ViewMode.ListElement)
export class BundleListElementComponent extends AbstractListableElementComponent<Bundle> {
}
