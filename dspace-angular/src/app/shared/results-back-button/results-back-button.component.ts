import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ds-results-back-button',
  styleUrls: ['./results-back-button.component.scss'],
  templateUrl: './results-back-button.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * Component for creating a back to result list button.
 */
export class ResultsBackButtonComponent implements OnInit {

  /**
   * The function used for back navigation.
   */
  @Input() back: () => void;

  /**
   * The button label translation.
   */
  @Input() buttonLabel?: Observable<any>;

  constructor(private translateService: TranslateService) {

  }

  ngOnInit(): void {
    if (!this.buttonLabel) {
      // Use the 'Back to Results' label as default.
      this.buttonLabel = this.translateService.get('search.browse.item-back');
    }
  }

}
