import { URLCombiner } from '../core/url-combiner/url-combiner';

export const PROCESS_MODULE_PATH = 'processes';

export const getProcessListRoute = () =>
    `/${PROCESS_MODULE_PATH}`;

export const getProcessDetailRoute = (processId: string) =>
    new URLCombiner(getProcessListRoute(), processId).toString();
