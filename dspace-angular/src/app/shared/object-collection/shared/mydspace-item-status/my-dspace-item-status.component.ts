import { Component, Input, OnInit } from '@angular/core';
import { MyDspaceItemStatusType } from './my-dspace-item-status-type';

/**
 * This component represents a badge with mydspace item status
 */
@Component({
  selector: 'ds-mydspace-item-status',
  styleUrls: ['./my-dspace-item-status.component.scss'],
  templateUrl: './my-dspace-item-status.component.html'
})
export class MyDSpaceItemStatusComponent implements OnInit {

  /**
   * This mydspace item status
   */
  @Input() status: MyDspaceItemStatusType;

  /**
   * This badge class
   */
  public badgeClass: string;

  /**
   * This badge content
   */
  public badgeContent: string;

  /**
   * Initialize badge content and class
   */
  ngOnInit() {
    this.badgeContent = this.status;
    this.badgeClass = 'text-light badge ';
    switch (this.status) {
      case MyDspaceItemStatusType.VALIDATION:
        this.badgeClass += 'badge-validation';
        break;
      case MyDspaceItemStatusType.WAITING_CONTROLLER:
        this.badgeClass += 'badge-waiting-controller';
        break;
      case MyDspaceItemStatusType.WORKSPACE:
        this.badgeClass += 'badge-workspace';
        break;
      case MyDspaceItemStatusType.ARCHIVED:
        this.badgeClass += 'badge-archived';
        break;
      case MyDspaceItemStatusType.WORKFLOW:
        this.badgeClass += 'badge-workflow';
        break;
    }
  }

}
