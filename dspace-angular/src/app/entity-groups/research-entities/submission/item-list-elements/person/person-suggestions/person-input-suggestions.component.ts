import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { InputSuggestionsComponent } from '../../../../../../shared/input-suggestions/input-suggestions.component';

@Component({
  selector: 'ds-person-input-suggestions',
  styleUrls: ['./person-input-suggestions.component.scss', './../../../../../../shared/input-suggestions/input-suggestions.component.scss'],
  templateUrl: './person-input-suggestions.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      // Usage of forwardRef necessary https://github.com/angular/angular.io/issues/1151
      // eslint-disable-next-line @angular-eslint/no-forward-ref
      useExisting: forwardRef(() => PersonInputSuggestionsComponent),
      multi: true
    }
  ]
})

/**
 * Component representing a form with a autocomplete functionality
 */
export class PersonInputSuggestionsComponent extends InputSuggestionsComponent implements OnInit {
  /**
   * The suggestions that should be shown
   */
  @Input() suggestions: string[] = [];

  ngOnInit() {
    if (this.suggestions.length > 0) {
      this.value = this.suggestions[0];
    }
  }

  onSubmit(data) {
    if (data !== this.value) {
      this.value = data;
      this.submitSuggestion.emit(data);
    }
  }

  onClickSuggestion(data) {
    this.value = data;
    this.clickSuggestion.emit(data);
    this.close();
    this.blockReopen = true;
    this.queryInput.nativeElement.focus();
    return false;
  }
}
