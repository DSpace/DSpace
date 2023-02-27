import { Component } from '@angular/core';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { AbstractListableElementComponent } from '../../../../shared/object-collection/shared/object-collection-element/abstract-listable-element.component';
import { Item } from '../../../../core/shared/item.model';

@listableObjectComponent('JournalIssue', ViewMode.GridElement)
@Component({
  selector: 'ds-journal-issue-grid-element',
  styleUrls: ['./journal-issue-grid-element.component.scss'],
  templateUrl: './journal-issue-grid-element.component.html',
})
/**
 * The component for displaying a grid element for an item of the type Journal Issue
 */
export class JournalIssueGridElementComponent extends AbstractListableElementComponent<Item> {
}
