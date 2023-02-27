import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'ds-sidebar-dropdown',
  styleUrls: ['./sidebar-dropdown.component.scss'],
  templateUrl: './sidebar-dropdown.component.html',
})
/**
 * This components renders a sidebar dropdown including the label.
 * The options should still be provided in the content.
 */
export class SidebarDropdownComponent {
  @Input() id: string;
  @Input() label: string;
  @Output() change: EventEmitter<any> = new EventEmitter<number>();
}
