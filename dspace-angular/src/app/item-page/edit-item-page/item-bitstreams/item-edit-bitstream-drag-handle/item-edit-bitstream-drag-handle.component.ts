import { Component, OnInit, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
  selector: 'ds-item-edit-bitstream-drag-handle',
  styleUrls: ['../item-bitstreams.component.scss'],
  templateUrl: './item-edit-bitstream-drag-handle.component.html',
})
/**
 * Component displaying a drag handle for the item-edit-bitstream page
 * Creates an embedded view of the contents
 * (which means it'll be added to the parents html without a wrapping ds-item-edit-bitstream-drag-handle element)
 */
export class ItemEditBitstreamDragHandleComponent implements OnInit {
  /**
   * The view on the drag-handle
   */
  @ViewChild('handleView', {static: true}) handleView;

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    this.viewContainerRef.createEmbeddedView(this.handleView);
  }

}
