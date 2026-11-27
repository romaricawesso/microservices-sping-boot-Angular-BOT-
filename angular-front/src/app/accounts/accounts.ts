import {Component, inject, signal} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {AsyncPipe, DatePipe} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {BehaviorSubject, catchError, map, Observable, of, switchMap} from 'rxjs';
import {Account, AccountListState, RequestStatus} from '../model/account.model';
import {Customer} from '../model/customer.model';
import {apiErrorMessage} from '../model/api-error';
import {Loadingservice} from '../servives/loading';

const ACCOUNTS_URL = "http://localhost:8080/EBANK-SERVICE/accounts";
const CUSTOMERS_URL = "http://localhost:8080/CUSTOMER-SERVICE/customers";

@Component({
  imports: [
    AsyncPipe,
    DatePipe,
    ReactiveFormsModule
  ],
  selector: 'app-accounts',
  styleUrl: './accounts.css',
  templateUrl: './accounts.html',
})

export class Accounts {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  public loadService = inject(Loadingservice);

  // chaque next() recharge la liste (apres un ajout par exemple)
  private refresh = new BehaviorSubject<void>(undefined);

  accounts: Observable<AccountListState> = this.refresh.pipe(
    switchMap(() => this.http.get<Account[]>(ACCOUNTS_URL).pipe(
      map(resp => {
        return {accounts: resp, status: RequestStatus.SUCCES}
      }),
      catchError((err: HttpErrorResponse) => {
        return of({status: RequestStatus.ERROR, errorMessage: apiErrorMessage(err)})
      })
    ))
  );

  // liste des clients pour choisir le titulaire du compte
  customers: Observable<Customer[]> = this.http.get<Customer[]>(CUSTOMERS_URL).pipe(
    catchError(() => of([]))
  );

  accountTypes = ['CURRENT', 'SAVING', 'BUSINESS'];

  accountForm = this.fb.group({
    customerId: [null as number | null, Validators.required],
    type: ['CURRENT', Validators.required],
    balance: [0, [Validators.required, Validators.min(0)]],
  });
  formError = signal<string | null>(null);
  formSuccess = signal<string | null>(null);

  saveAccount() {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }
    this.formError.set(null);
    this.formSuccess.set(null);
    this.http.post<Account>(ACCOUNTS_URL, this.accountForm.getRawValue()).subscribe({
      next: account => {
        this.formSuccess.set(`Compte ${account.type} cree pour le client ${account.customerId}`);
        this.accountForm.reset({customerId: null, type: 'CURRENT', balance: 0});
        this.refresh.next();
      },
      error: (err: HttpErrorResponse) => this.formError.set(apiErrorMessage(err))
    });
  }

  protected readonly RequestStatus = RequestStatus;
}
