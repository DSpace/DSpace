import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageNotFoundComponent as BaseComponent } from '../../../../app/pagenotfound/pagenotfound.component';

@Component({
  selector: 'ds-pagenotfound',
  // styleUrls: ['./pagenotfound.component.scss'],
  styleUrls: ['../../../../app/pagenotfound/pagenotfound.component.scss'],
  // templateUrl: './pagenotfound.component.html'
  templateUrl: '../../../../app/pagenotfound/pagenotfound.component.html',
  changeDetection: ChangeDetectionStrategy.Default
})

/**
 * This component representing the `PageNotFound` DSpace page.
 */
export class PageNotFoundComponent extends BaseComponent {}

