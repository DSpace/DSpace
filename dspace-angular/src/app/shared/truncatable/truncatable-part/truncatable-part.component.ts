import { AfterViewChecked, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TruncatableService } from '../truncatable.service';
import { hasValue } from '../../empty.util';

@Component({
  selector: 'ds-truncatable-part',
  templateUrl: './truncatable-part.component.html',
  styleUrls: ['./truncatable-part.component.scss']
})

/**
 * Component that truncates/clamps a piece of text
 * It needs a TruncatableComponent parent to identify it's current state
 */
export class TruncatablePartComponent implements AfterViewChecked, OnInit, OnDestroy {
  /**
   * Number of lines shown when the part is collapsed
   */
  @Input() minLines: number;

  /**
   * Number of lines shown when the part is expanded. -1 indicates no limit
   */
  @Input() maxLines = -1;

  /**
   * The identifier of the parent TruncatableComponent
   */
  @Input() id: string;

  /**
   * Type of text, can be a h4 for headers or any other class you want to add
   */
  @Input() type: string;

  /**
   * True if the minimal height of the part should at least be as high as it's minimum amount of lines
   */
  @Input() fixedHeight = false;

  @Input() background = 'default';

  /**
   * A boolean representing if to show or not the show/collapse toggle.
   * This value must have the same value as the parent TruncatableComponent
   */
  @Input() showToggle = true;

  /**
   * The view on the truncatable part
   */
  @ViewChild('content', {static: true}) content: ElementRef;

  /**
   * Current amount of lines shown of this part
   */
  lines: string;

  /**
   * Subscription to unsubscribe from
   */
  private sub;
  /**
   * store variable used for local to expand collapse
   */
  expand = false;
  /**
   * variable to check if expandable
   */
  expandable = false;

  public constructor(private service: TruncatableService) {}

  /**
   * Initialize lines variable
   */
  ngOnInit() {
    this.setLines();
  }

  /**
   * Subscribe to the current state to determine how much lines should be shown of this part
   */
  private setLines() {
    this.sub = this.service.isCollapsed(this.id).subscribe((collapsed: boolean) => {
      if (collapsed) {
        this.lines = this.minLines.toString();
        this.expand = false;
      } else {
        this.lines = this.maxLines < 0 ? 'none' : this.maxLines.toString();
        this.expand = true;
      }
    });
  }

  ngAfterViewChecked() {
    this.truncateElement();
  }

  /**
   * Expands the truncatable when it's collapsed, collapses it when it's expanded
   */
  public toggle() {
    this.service.toggle(this.id);
    this.expandable = !this.expandable;
  }

  /**
   * check for the truncate element
   */
  public truncateElement() {
    if (this.showToggle) {
      const entry = this.content.nativeElement;
      if (entry.scrollHeight > entry.offsetHeight) {
        if (entry.children.length > 0) {
          if (entry.children[entry.children.length - 1].offsetHeight > entry.offsetHeight) {
            entry.classList.add('truncated');
            entry.classList.remove('removeFaded');
          } else {
            entry.classList.remove('truncated');
            entry.classList.add('removeFaded');
          }
        } else {
          if (entry.innerText.length > 0) {
            entry.classList.add('truncated');
            entry.classList.remove('removeFaded');
          } else {
            entry.classList.remove('truncated');
            entry.classList.add('removeFaded');
          }
        }
      } else {
        entry.classList.remove('truncated');
        entry.classList.add('removeFaded');
      }
    }
  }

  /**
   * Unsubscribe from the subscription
   */
  ngOnDestroy(): void {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }
}
