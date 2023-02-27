import { Config } from './config.interface';

export interface ItemConfig extends Config {
  edit: {
    undoTimeout: number;
  };
  // This is used to show the access status label of items in results lists
  showAccessStatuses: boolean;

  bitstream: {
    // Number of entries in the bitstream list in the item view page.
    // Rounded to the nearest size in the list of selectable sizes on the
    // settings menu.  See pageSizeOptions in 'pagination-component-options.model.ts'.
    pageSize: number;
  }
}
