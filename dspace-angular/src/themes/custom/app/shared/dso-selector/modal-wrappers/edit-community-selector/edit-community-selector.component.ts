import { Component } from '@angular/core';
import {
    EditCommunitySelectorComponent as BaseComponent
} from '../../../../../../../app/shared/dso-selector/modal-wrappers/edit-community-selector/edit-community-selector.component';

@Component({
  selector: 'ds-edit-item-selector',
  // styleUrls: ['./edit-community-selector.component.scss'],
  // templateUrl: './edit-community-selector.component.html',
  templateUrl: '../../../../../../../app/shared/dso-selector/modal-wrappers/dso-selector-modal-wrapper.component.html',
})
export class EditCommunitySelectorComponent extends BaseComponent {
}
