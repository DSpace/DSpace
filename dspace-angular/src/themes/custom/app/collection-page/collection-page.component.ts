import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CollectionPageComponent as BaseComponent} from '../../../../app/collection-page/collection-page.component';
import { fadeIn, fadeInOut } from '../../../../app/shared/animations/fade';


@Component({
  selector: 'ds-collection-page',
  // templateUrl: './collection-page.component.html',
  templateUrl: '../../../../app/collection-page/collection-page.component.html',
  // styleUrls: ['./collection-page.component.scss']
  styleUrls: ['../../../../app/collection-page/collection-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    fadeIn,
    fadeInOut
  ]
})
/**
 * This component represents a detail page for a single collection
 */
export class CollectionPageComponent extends BaseComponent {}
