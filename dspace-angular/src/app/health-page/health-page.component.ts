import { Component, OnInit } from '@angular/core';

import { BehaviorSubject } from 'rxjs';
import { take } from 'rxjs/operators';

import { HealthService } from './health.service';
import { HealthInfoResponse, HealthResponse } from './models/health-component.model';

@Component({
  selector: 'ds-health-page',
  templateUrl: './health-page.component.html',
  styleUrls: ['./health-page.component.scss']
})
export class HealthPageComponent implements OnInit {

  /**
   * Health info endpoint response
   */
  healthInfoResponse: BehaviorSubject<HealthInfoResponse> = new BehaviorSubject<HealthInfoResponse>(null);

  /**
   * Health endpoint response
   */
  healthResponse: BehaviorSubject<HealthResponse> = new BehaviorSubject<HealthResponse>(null);

  /**
   * Represent if the response from health status endpoint is already retrieved or not
   */
  healthResponseInitialised: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * Represent if the response from health info endpoint is already retrieved or not
   */
  healthInfoResponseInitialised: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  constructor(private healthDataService: HealthService) {
  }

  /**
   * Retrieve responses from rest
   */
  ngOnInit(): void {
    this.healthDataService.getHealth().pipe(take(1)).subscribe({
      next: (data: any) => {
        this.healthResponse.next(data.payload);
        this.healthResponseInitialised.next(true);
      },
      error: () => {
        this.healthResponse.next(null);
        this.healthResponseInitialised.next(true);
      }
    });

    this.healthDataService.getInfo().pipe(take(1)).subscribe({
      next: (data: any) => {
        this.healthInfoResponse.next(data.payload);
        this.healthInfoResponseInitialised.next(true);
      },
      error: () => {
        this.healthInfoResponse.next(null);
        this.healthInfoResponseInitialised.next(true);
      }
    });

  }
}
