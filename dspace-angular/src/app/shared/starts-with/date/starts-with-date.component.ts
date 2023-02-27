import { Component, Inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { renderStartsWithFor, StartsWithType } from '../starts-with-decorator';
import { StartsWithAbstractComponent } from '../starts-with-abstract.component';
import { hasValue } from '../../empty.util';
import { PaginationService } from '../../../core/pagination/pagination.service';

/**
 * A switchable component rendering StartsWith options for the type "Date".
 * The options are rendered in a dropdown with an input field (of type number) next to it.
 */
@Component({
  selector: 'ds-starts-with-date',
  styleUrls: ['./starts-with-date.component.scss'],
  templateUrl: './starts-with-date.component.html'
})
@renderStartsWithFor(StartsWithType.date)
export class StartsWithDateComponent extends StartsWithAbstractComponent {

  /**
   * A list of options for months to select from
   */
  monthOptions: string[];

  /**
   * Currently selected month
   */
  startsWithMonth = 'none';

  /**
   * Currently selected year
   */
  startsWithYear: number;

  public constructor(@Inject('startsWithOptions') public startsWithOptions: any[],
                     @Inject('paginationId') public paginationId: string,
                     protected paginationService: PaginationService,
                     protected route: ActivatedRoute,
                     protected router: Router) {
    super(startsWithOptions, paginationId, paginationService, route, router);
  }

  ngOnInit() {
    this.monthOptions = [
      'none',
      'january',
      'february',
      'march',
      'april',
      'may',
      'june',
      'july',
      'august',
      'september',
      'october',
      'november',
      'december'
    ];

    super.ngOnInit();
  }

  /**
   * Set the startsWith by event
   * @param event
   */
  setStartsWithYearEvent(event: Event) {
    this.startsWithYear = +(event.target as HTMLInputElement).value;
    this.setStartsWithYearMonth();
    this.setStartsWithParam(true);
  }

  /**
   * Set the startsWithMonth by event
   * @param event
   */
  setStartsWithMonthEvent(event: Event) {
    this.startsWithMonth = (event.target as HTMLInputElement).value;
    this.setStartsWithYearMonth();
    this.setStartsWithParam(true);
  }

  /**
   * Get startsWith year combined with month;
   * Returned value: "{{year}}-{{month}}"
   */
  getStartsWith() {
    const month = this.getStartsWithMonth();
    if (month > 0 && hasValue(this.startsWithYear) && this.startsWithYear !== -1) {
      let twoDigitMonth = '' + month;
      if (month < 10) {
        twoDigitMonth = `0${month}`;
      }
      return `${this.startsWithYear}-${twoDigitMonth}`;
    } else {
      if (hasValue(this.startsWithYear) && this.startsWithYear > 0) {
        return '' + this.startsWithYear;
      } else {
        return undefined;
      }
    }
  }

  /**
   * Set startsWith year combined with month;
   */
  setStartsWithYearMonth() {
    this.startsWith = this.getStartsWith();
  }

  /**
   * Set the startsWith by string
   * This method also sets startsWithYear and startsWithMonth correctly depending on the received value
   * - When startsWith contains a "-", the first part is considered the year, the second part the month
   * - When startsWith doesn't contain a "-", the whole string is expected to be the year
   * startsWithMonth will be set depending on the index received after the "-"
   * @param startsWith
   */
  setStartsWith(startsWith: string) {
    this.startsWith = startsWith;
    if (hasValue(startsWith) && startsWith.indexOf('-') > -1) {
      const split = startsWith.split('-');
      this.startsWithYear = +split[0];
      const month = +split[1];
      if (month < this.monthOptions.length) {
        this.startsWithMonth = this.monthOptions[month];
      } else {
        this.startsWithMonth = this.monthOptions[0];
      }
    } else {
      this.startsWithYear = +startsWith;
    }
    this.setStartsWithParam(false);
  }

  /**
   * Get startsWithYear as a number;
   */
  getStartsWithYear() {
    return this.startsWithYear;
  }

  /**
   * Get startsWithMonth as a number;
   */
  getStartsWithMonth() {
    return this.monthOptions.indexOf(this.startsWithMonth);
  }

}
