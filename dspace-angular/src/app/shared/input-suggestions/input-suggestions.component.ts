import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  QueryList,
  SimpleChanges,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { hasValue, isNotEmpty } from '../empty.util';
import { ControlValueAccessor } from '@angular/forms';

@Component({
  selector: 'ds-input-suggestions',
  templateUrl: './input-suggestions.component.html',
})

/**
 * Component representing a form with a autocomplete functionality
 */
export class InputSuggestionsComponent implements ControlValueAccessor, OnChanges {
  /**
   * The suggestions that should be shown
   */
  @Input() suggestions: any[] = [];

  /**
   * The time waited to detect if any other input will follow before requesting the suggestions
   */
  @Input() debounceTime = 500;

  /**
   * Placeholder attribute for the input field
   */
  @Input() placeholder = '';

  /**
   * Action attribute for the form
   */
  @Input() action;

  /**
   * Name attribute for the input field
   */
  @Input() name;

  /**
   * Whether or not the current input is valid
   */
  @Input() valid = true;

  /**
   * Label for the input field. Used for screen readers.
   */
  @Input() label? = '';

  /**
   * Output for when the form is submitted
   */
  @Output() submitSuggestion = new EventEmitter();

  /**
   * Output for when a suggestion is clicked
   */
  @Output() clickSuggestion = new EventEmitter();

  /**
   * Output for when something is typed in the input field
   */
  @Output() typeSuggestion = new EventEmitter();

  /**
   * Output for when new suggestions should be requested
   */
  @Output() findSuggestions = new EventEmitter();

  /**
   * Emits true when the list of suggestions should be shown
   */
  show = new BehaviorSubject<boolean>(false);

  /**
   * Index of the currently selected suggestion
   */
  selectedIndex = -1;

  /**
   * True when the dropdown should not reopen
   */
  blockReopen = false;

  /**
   * Reference to the input field component
   */
  @ViewChild('inputField') queryInput: ElementRef;
  /**
   * Reference to the suggestion components
   */
  @ViewChildren('suggestion') resultViews: QueryList<ElementRef>;

  /**
   * Value of the input field
   */
  _value: string;

  /** Fields needed to add ngModel */
  @Input() disabled = false;
  propagateChange = (_: any) => {
    /* Empty implementation */
  };
  propagateTouch = (_: any) => {
    /* Empty implementation */
  };

  /**
   * When any of the inputs change, check if we should still show the suggestions
   */
  ngOnChanges(changes: SimpleChanges) {
    if (hasValue(changes.suggestions)) {
      this.show.next(isNotEmpty(changes.suggestions.currentValue) && !changes.suggestions.firstChange);
    }
  }

  /**
   * Move the focus on one of the suggestions up to the previous suggestion
   * When no suggestion is currently in focus OR the first suggestion is in focus: shift to the last suggestion
   */
  shiftFocusUp(event: KeyboardEvent) {
    event.preventDefault();
    if (this.selectedIndex > 0) {
      this.selectedIndex--;
      this.selectedIndex = (this.selectedIndex + this.resultViews.length) % this.resultViews.length; // Prevent negative modulo outcome
    } else {
      this.selectedIndex = this.resultViews.length - 1;
    }
    this.changeFocus();
  }

  /**
   * Move the focus on one of the suggestions up to the next suggestion
   * When no suggestion is currently in focus OR the last suggestion is in focus: shift to the first suggestion
   */
  shiftFocusDown(event: KeyboardEvent) {
    event.preventDefault();
    if (this.selectedIndex >= 0) {
      this.selectedIndex++;
      this.selectedIndex %= this.resultViews.length;
    } else {
      this.selectedIndex = 0;
    }
    this.changeFocus();
  }

  /**
   * Perform the change of focus to the current selectedIndex
   */
  changeFocus() {
    if (this.resultViews.length > 0) {
      this.resultViews.toArray()[this.selectedIndex].nativeElement.focus();
    }
  }

  /**
   * When any key is pressed (except for the Enter button) the query input should move to the input field
   * @param {KeyboardEvent} event The keyboard event
   */
  onKeydown(event: KeyboardEvent) {
    if (event.key !== 'Enter') {
      this.queryInput.nativeElement.focus();
    }
  }

  /**
   * Changes the show variable so the suggestion dropdown closes
   */
  close() {
    this.show.next(false);
  }

  /**
   * Changes the show variable so the suggestion dropdown opens
   */
  open() {
    if (!this.blockReopen) {
      this.show.next(true);
    }
  }

  /**
   * For usage of the isNotEmpty function in the template
   */
  isNotEmpty(data) {
    return isNotEmpty(data);
  }

  onSubmit(data: any) {
    // sub class should decide how to handle the date
  }

  /**
   * Make sure that if a suggestion is clicked, the suggestions dropdown closes, does not reopen and the focus moves to the input field
   */
  onClickSuggestion(data: any) {
    // sub class should decide how to handle the date
  }

  /**
   * Finds new suggestions when necessary
   * @param data The query value to emit
   */
  find(data) {
    if (!this.blockReopen) {
      this.findSuggestions.emit(data);
      this.typeSuggestion.emit(data);
    }
    this.blockReopen = false;
  }

  /* START - Method's needed to add ngModel (ControlValueAccessor) to a component */
  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.propagateTouch = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.value = value;
  }

  get value() {
    return this._value;
  }

  set value(val) {
    this._value = val;
    this.propagateChange(this._value);
  }
  /* END - Method's needed to add ngModel to a component */
}
