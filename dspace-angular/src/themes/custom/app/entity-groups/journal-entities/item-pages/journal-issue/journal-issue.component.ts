import { Component } from '@angular/core';
import { ViewMode } from '../../../../../../../app/core/shared/view-mode.model';
import {
  listableObjectComponent
} from '../../../../../../../app/shared/object-collection/shared/listable-object/listable-object.decorator';
import {
  JournalIssueComponent as BaseComponent
} from '../../../../../../../app/entity-groups/journal-entities/item-pages/journal-issue/journal-issue.component';
import { Context } from '../../../../../../../app/core/shared/context.model';

@listableObjectComponent('JournalIssue', ViewMode.StandalonePage, Context.Any, 'custom')
@Component({
  selector: 'ds-journal-issue',
  // styleUrls: ['./journal-issue.component.scss'],
  styleUrls: ['../../../../../../../app/entity-groups/journal-entities/item-pages/journal-issue/journal-issue.component.scss'],
  // templateUrl: './journal-issue.component.html',
  templateUrl: '../../../../../../../app/entity-groups/journal-entities/item-pages/journal-issue/journal-issue.component.html',
})
/**
 * The component for displaying metadata and relations of an item of the type Journal Issue
 */
export class JournalIssueComponent extends BaseComponent {
}
