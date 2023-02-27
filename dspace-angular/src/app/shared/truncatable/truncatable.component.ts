import { AfterViewChecked, Component, ElementRef, Input, OnInit } from '@angular/core';
import { TruncatableService } from './truncatable.service';

@Component({
  selector: 'ds-truncatable',
  templateUrl: './truncatable.component.html',
  styleUrls: ['./truncatable.component.scss'],

})

/**
 * Component that represents a section with one or more truncatable parts that all listen to this state
 */
export class TruncatableComponent implements OnInit, AfterViewChecked {
  /**
   * Is true when all truncatable parts in this truncatable should be expanded on loading
   */
  @Input() initialExpand = false;

  /**
   * The unique identifier of this truncatable component
   */
  @Input() id: string;

  /**
   * Is true when the truncatable should expand on both hover as click
   */
  @Input() onHover = false;

  /**
   * A boolean representing if to show or not the show/collapse toggle
   * This value must have the same value as the children TruncatablePartComponent
   */
  @Input() showToggle = true;

  public constructor(private service: TruncatableService, private el: ElementRef,) {
  }

  /**
   * Set the initial state
   */
  ngOnInit() {
    if (this.initialExpand) {
      this.service.expand(this.id);
    } else {
      this.service.collapse(this.id);
    }
  }

  /**
   * If onHover is true, collapses the truncatable
   */
  public hoverCollapse() {
    if (this.onHover) {
      this.service.collapse(this.id);
    }
  }

  /**
   * If onHover is true, expands the truncatable
   */
  public hoverExpand() {
    if (this.onHover) {
      this.service.expand(this.id);
    }
  }

  ngAfterViewChecked() {
    if (this.showToggle) {
      const truncatedElements = this.el.nativeElement.querySelectorAll('.truncated');
      if (truncatedElements?.length > 0) {
        const truncateElements = this.el.nativeElement.querySelectorAll('.dont-break-out');
        for (let i = 0; i < (truncateElements.length - 1); i++) {
          truncateElements[i].classList.remove('truncated');
          truncateElements[i].classList.add('notruncatable');
        }
        truncateElements[truncateElements.length - 1].classList.add('truncated');
      }
    }
  }

}
