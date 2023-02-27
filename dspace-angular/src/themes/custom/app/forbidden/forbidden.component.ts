import { Component } from '@angular/core';
import { ForbiddenComponent as BaseComponent } from '../../../../app/forbidden/forbidden.component';


@Component({
  selector: 'ds-forbidden',
  // templateUrl: './forbidden.component.html',
  templateUrl: '../../../../app/forbidden/forbidden.component.html',
  // styleUrls: ['./forbidden.component.scss']
  styleUrls: ['../../../../app/forbidden/forbidden.component.scss']
})
/**
 * This component representing the `Forbidden` DSpace page.
 */
export class ForbiddenComponent extends BaseComponent {}
