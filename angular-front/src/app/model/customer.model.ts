import {RequestStatus} from './account.model';

export interface Customer {
  id: number,
  name: string,
  email: string
}

export interface CustomerListState {
  customers?: Customer[],
  status?: RequestStatus,
  errorMessage?: string,
}
