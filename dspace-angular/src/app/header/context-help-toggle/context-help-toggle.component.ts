import { Component, OnInit } from '@angular/core';
import { ContextHelpService } from '../../shared/context-help.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * Renders a "context help toggle" button that toggles the visibility of tooltip buttons on the page.
 * If there are no tooltip buttons available on the current page, the toggle is unclickable.
 */
@Component({
  selector: 'ds-context-help-toggle',
  templateUrl: './context-help-toggle.component.html',
  styleUrls: ['./context-help-toggle.component.scss']
})
export class ContextHelpToggleComponent implements OnInit {
  buttonVisible$: Observable<boolean>;

  constructor(
    private contextHelpService: ContextHelpService,
  ) { }

  ngOnInit(): void {
    this.buttonVisible$ = this.contextHelpService.tooltipCount$().pipe(map(x => x > 0));
  }

  onClick() {
    this.contextHelpService.toggleIcons();
  }
}
