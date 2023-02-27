import { Component } from '@angular/core';
import { ViewMode } from '../../../../../../../app/core/shared/view-mode.model';
import {
  listableObjectComponent
} from '../../../../../../../app/shared/object-collection/shared/listable-object/listable-object.decorator';
import {
  JournalVolumeComponent as BaseComponent
} from '../../../../../../../app/entity-groups/journal-entities/item-pages/journal-volume/journal-volume.component';
import { Context } from '../../../../../../../app/core/shared/context.model';

@listableObjectComponent('JournalVolume', ViewMode.StandalonePage, Context.Any, 'custom')
@Component({
  selector: 'ds-journal-volume',
  // styleUrls: ['./journal-volume.component.scss'],
  styleUrls: ['../../../../../../../app/entity-groups/journal-entities/item-pages/journal-volume/journal-volume.component.scss'],
  // templateUrl: './journal-volume.component.html',
  templateUrl: '../../../../../../../app/entity-groups/journal-entities/item-pages/journal-volume/journal-volume.component.html',
})
/**
 * The component for displaying metadata and relations of an item of the type Journal Volume
 */
export class JournalVolumeComponent extends BaseComponent {
}
