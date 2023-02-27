import {Component} from '@angular/core';

import { Community } from '../../../../../../app/core/shared/community.model';
import {
  CommunityListElementComponent as BaseComponent
} from '../../../../../../app/shared/object-list/community-list-element/community-list-element.component';
import { ViewMode } from '../../../../../../app/core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../../../app/shared/object-collection/shared/listable-object/listable-object.decorator';
import {Context} from '../../../../../../app/core/shared/context.model';

@listableObjectComponent(Community,  ViewMode.ListElement, Context.Any, 'custom')

@Component({
  selector: 'ds-community-list-element',
  // styleUrls: ['./community-list-element.component.scss'],
  styleUrls: ['../../../../../../app/shared/object-list/community-list-element/community-list-element.component.scss'],
  // templateUrl: './community-list-element.component.html'
  templateUrl: '../../../../../../app/shared/object-list/community-list-element/community-list-element.component.html'
})
/**
 * Component representing a list element for a community
 */
export class CommunityListElementComponent extends BaseComponent {}
