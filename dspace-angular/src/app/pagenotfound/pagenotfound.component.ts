import { ServerResponseService } from '../core/services/server-response.service';
import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { AuthService } from '../core/auth/auth.service';

/**
 * This component representing the `PageNotFound` DSpace page.
 */
@Component({
  selector: 'ds-pagenotfound',
  styleUrls: ['./pagenotfound.component.scss'],
  templateUrl: './pagenotfound.component.html',
  changeDetection: ChangeDetectionStrategy.Default
})
export class PageNotFoundComponent implements OnInit {

  /**
   * Initialize instance variables
   *
   * @param {AuthService} authservice
   * @param {ServerResponseService} responseService
   */
  constructor(private authservice: AuthService, private responseService: ServerResponseService) {
    this.responseService.setNotFound();
  }

  /**
   * Remove redirect url from the state
   */
  ngOnInit(): void {
    this.authservice.clearRedirectUrl();
  }

}
