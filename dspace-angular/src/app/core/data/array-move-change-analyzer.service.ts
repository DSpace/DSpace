import { MoveOperation } from 'fast-json-patch';
import { Injectable } from '@angular/core';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { hasValue } from '../../shared/empty.util';

/**
 * A class to determine move operations between two arrays
 */
@Injectable()
export class ArrayMoveChangeAnalyzer<T> {

  /**
   * Compare two arrays detecting and returning move operations
   *
   * @param array1  The original array
   * @param array2  The custom array to compare with the original
   */
  diff(array1: T[], array2: T[]): MoveOperation[] {
    return this.getMoves(array1, array2).map((move) => Object.assign({
      op: 'move',
      from: '/' + move[0],
      path: '/' + move[1],
    }) as MoveOperation);
  }

  /**
   * Determine a set of moves required to transform array1 into array2
   * The moves are returned as an array of pairs of numbers where the first number is the original index and the second
   * is the new index
   * It is assumed the operations are executed in the order they're returned (and not simultaneously)
   * @param array1
   * @param array2
   */
  private getMoves(array1: any[], array2: any[]): number[][] {
    const moved = [...array2];

    return array1.reduce((moves, item, index) => {
      if (hasValue(item) && item !== moved[index]) {
        const last = moved.lastIndexOf(item);
        moveItemInArray(moved, last, index);
        moves.unshift([index, last]);
      }
      return moves;
    }, []);
  }
}
