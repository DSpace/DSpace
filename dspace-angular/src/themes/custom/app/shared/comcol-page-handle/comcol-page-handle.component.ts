import { Component } from '@angular/core';
import { ComcolPageHandleComponent as BaseComponent} from '../../../../../app/shared/comcol/comcol-page-handle/comcol-page-handle.component';


/**
 * This component builds a URL from the value of "handle"
 */

@Component({
  selector: 'ds-comcol-page-handle',
  // templateUrl: './comcol-page-handle.component.html',
  templateUrl: '../../../../../app/shared/comcol/comcol-page-handle/comcol-page-handle.component.html',
  // styleUrls: ['./comcol-page-handle.component.scss'],
  styleUrls: ['../../../../../app/shared/comcol/comcol-page-handle/comcol-page-handle.component.scss'],
})


export class ComcolPageHandleComponent extends BaseComponent {}
