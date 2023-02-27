import { Component } from '@angular/core';
import {
    CreateCollectionParentSelectorComponent as BaseComponent
} from '../../../../../../../app/shared/dso-selector/modal-wrappers/create-collection-parent-selector/create-collection-parent-selector.component';

@Component({
  selector: 'ds-create-collection-parent-selector',
  // styleUrls: ['./create-collection-parent-selector.component.scss'],
  // templateUrl: './create-collection-parent-selector.component.html',
  templateUrl: '../../../../../../../app/shared/dso-selector/modal-wrappers/dso-selector-modal-wrapper.component.html',
})
export class CreateCollectionParentSelectorComponent extends BaseComponent {
}
