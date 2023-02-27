export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC'
}

export class SortOptions {
  constructor(public field: string, public direction: SortDirection) {

  }
}
