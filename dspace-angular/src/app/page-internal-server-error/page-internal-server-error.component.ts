import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ServerResponseService } from '../core/services/server-response.service';

/**
 * This component representing the `PageInternalServer` DSpace page.
 */
@Component({
  selector: 'ds-page-internal-server-error',
  styleUrls: ['./page-internal-server-error.component.scss'],
  templateUrl: './page-internal-server-error.component.html',
  changeDetection: ChangeDetectionStrategy.Default
})
export class PageInternalServerErrorComponent {

  /**
   * Initialize instance variables
   *
   * @param {ServerResponseService} responseService
   */
  constructor(private responseService: ServerResponseService) {
    this.responseService.setInternalServerError();
  }
}
