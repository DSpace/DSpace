import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, SimpleChanges, } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isEmpty } from '../../empty.util';

@Component({
  selector: 'ds-number-picker',
  styleUrls: ['./number-picker.component.scss'],
  templateUrl: './number-picker.component.html',
  providers: [
    {provide: NG_VALUE_ACCESSOR, useExisting: NumberPickerComponent, multi: true}
  ],
})

export class NumberPickerComponent implements OnInit, ControlValueAccessor {
  @Input() id: string;
  @Input() step: number;
  @Input() min: number;
  @Input() max: number;
  @Input() size: number;
  @Input() placeholder: string;
  @Input() name: string;
  @Input() disabled: boolean;
  @Input() invalid: boolean;
  @Input() value: number;

  @Output() selected = new EventEmitter<number>();
  @Output() remove = new EventEmitter<number>();
  @Output() blur = new EventEmitter<any>();
  @Output() change = new EventEmitter<any>();
  @Output() focus = new EventEmitter<any>();

  startValue: number;

  constructor(private fb: FormBuilder, private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    // this.startValue = this.value;
    this.step = this.step || 1;
    this.min = this.min || 0;
    this.max = this.max || 100;
    this.size = this.size || 1;
    this.disabled = this.disabled || false;
    this.invalid = this.invalid || false;
    this.cd.detectChanges();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.value) {
      if (changes.max) {
        // When the user select a month with < # of days
        this.value = this.value > this.max ? this.max : this.value;
      }

    } else if (changes.value && changes.value.currentValue === null) {
      // When the user delete the inserted value
        this.value = null;
    } else if (changes.invalid) {
      this.invalid = changes.invalid.currentValue;
    }
  }

  private changeValue(reverse: boolean = false) {

    // First after init
    if (isEmpty(this.value)) {
      this.value = this.startValue;
    } else {
      this.startValue = this.value;

      let newValue = this.value;
      if (reverse) {
        newValue -= this.step;
      } else {
        newValue += this.step;
      }

      if (newValue >= this.min && newValue <= this.max) {
        this.value = newValue;
      } else {
        if (newValue > this.max) {
          this.value = this.min;
        } else {
          this.value = this.max;
        }
      }
    }

    this.emitChange();
  }

  toggleDown() {
    this.changeValue(true);
  }

  toggleUp() {
    this.changeValue();
  }

  update(event) {
    try {
      const i = Number.parseInt(event.target.value, 10);

      if (i >= this.min && i <= this.max) {
        this.value = i;
        this.emitChange();
      } else if (event.target.value === null || event.target.value === '') {
        this.value = null;
        this.emitChange();
      } else {
        this.value = undefined;
      }
    } catch (e) {
      this.value = undefined;
    }
  }

  onBlur(event) {
    this.blur.emit(event);
  }

  onFocus(event) {
    if (this.value) {
      this.startValue = this.value;
    }
    this.focus.emit(event);
  }

  writeValue(value) {
    this.startValue = value || this.min;
  }

  registerOnChange(fn) {
    return;
  }

  registerOnTouched(fn) {
    return;
  }

  emitChange() {
    this.change.emit({field: this.name, value: this.value});
  }
}
