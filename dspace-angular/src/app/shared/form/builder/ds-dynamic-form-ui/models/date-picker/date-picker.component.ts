import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DynamicDsDatePickerModel } from './date-picker.model';
import { hasValue } from '../../../../../empty.util';
import {
  DynamicFormControlComponent,
  DynamicFormLayoutService,
  DynamicFormValidationService
} from '@ng-dynamic-forms/core';

export const DS_DATE_PICKER_SEPARATOR = '-';

@Component({
  selector: 'ds-date-picker',
  styleUrls: ['./date-picker.component.scss'],
  templateUrl: './date-picker.component.html',
})

export class DsDatePickerComponent extends DynamicFormControlComponent implements OnInit {
  @Input() bindId = true;
  @Input() group: FormGroup;
  @Input() model: DynamicDsDatePickerModel;
  @Input() legend: string;

  @Output() selected = new EventEmitter<number>();
  @Output() remove = new EventEmitter<number>();
  @Output() blur = new EventEmitter<any>();
  @Output() change = new EventEmitter<any>();
  @Output() focus = new EventEmitter<any>();

  initialYear: number;
  initialMonth: number;
  initialDay: number;

  year: any;
  month: any;
  day: any;

  minYear: 0;
  maxYear: number;
  minMonth = 1;
  maxMonth = 12;
  minDay = 1;
  maxDay = 31;

  yearPlaceholder = 'year';
  monthPlaceholder = 'month';
  dayPlaceholder = 'day';

  disabledMonth = true;
  disabledDay = true;

  constructor(protected layoutService: DynamicFormLayoutService,
              protected validationService: DynamicFormValidationService
  ) {
    super(layoutService, validationService);
  }

  ngOnInit() {
    const now = new Date();
    this.initialYear = now.getUTCFullYear();
    this.initialMonth = now.getUTCMonth() + 1;
    this.initialDay = now.getUTCDate();

    if (this.model && this.model.value !== null) {
      const values = this.model.value.toString().split(DS_DATE_PICKER_SEPARATOR);
      if (values.length > 0) {
        this.initialYear = parseInt(values[0], 10);
        this.year = this.initialYear;
        this.disabledMonth = false;
      }
      if (values.length > 1) {
        this.initialMonth = parseInt(values[1], 10);
        this.month = this.initialMonth;
        this.disabledDay = false;
      }
      if (values.length > 2) {
        this.initialDay = parseInt(values[2], 10);
        this.day = this.initialDay;
      }
    }

    this.maxYear = this.initialYear + 100;

  }

  onBlur(event) {
    this.blur.emit();
  }

  onChange(event) {
    // update year-month-day
    switch (event.field) {
      case 'year': {
        if (event.value !== null) {
          this.year = event.value;
        } else {
          this.year = undefined;
          this.month = undefined;
          this.day = undefined;
          this.disabledMonth = true;
          this.disabledDay = true;
        }
        break;
      }
      case 'month': {
        if (event.value !== null) {
          this.month = event.value;
        } else {
          this.month = undefined;
          this.day = undefined;
          this.disabledDay = true;
        }
        break;
      }
      case 'day': {
        if (event.value !== null) {
          this.day = event.value;
        } else {
          this.day = undefined;
        }
        break;
      }
    }

    // set max for days by month/year
    if (!this.disabledDay) {
      const month = this.month ? this.month - 1 : 0;
      const date = new Date(this.year, month, 1);
      this.maxDay = this.getLastDay(date);
      if (this.day > this.maxDay) {
        this.day = this.maxDay;
      }
    }

    // Manage disable
    if (hasValue(this.year) && event.field === 'year') {
      this.disabledMonth = false;
    } else if (hasValue(this.month) && event.field === 'month') {
      this.disabledDay = false;
    }

    // update value
    let value = null;
    if (hasValue(this.year)) {
      let yyyy = this.year.toString();
      while (yyyy.length < 4) {
        yyyy = '0' + yyyy;
      }
      value = yyyy;
    }
    if (hasValue(this.month)) {
      const mm = this.month.toString().length === 1
        ? '0' + this.month.toString()
        : this.month.toString();
      value += DS_DATE_PICKER_SEPARATOR + mm;
    }
    if (hasValue(this.day)) {
      const dd = this.day.toString().length === 1
        ? '0' + this.day.toString()
        : this.day.toString();
      value += DS_DATE_PICKER_SEPARATOR + dd;
    }

    this.model.value = value;
    this.change.emit(value);
  }

  onFocus(event) {
    this.focus.emit(event);
  }

  getLastDay(date: Date) {
    // Last Day of the same month (+1 month, -1 day)
    date.setMonth(date.getMonth() + 1, 0);
    return date.getDate();
  }

}
