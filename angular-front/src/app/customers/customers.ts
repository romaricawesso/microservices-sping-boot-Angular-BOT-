import {Component, inject, signal} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {AsyncPipe} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {BehaviorSubject, catchError, map, Observable, of, switchMap} from 'rxjs';
import {RequestStatus} from '../model/account.model';
import {Customer, CustomerListState} from '../model/customer.model';
import {apiErrorMessage} from '../model/api-error';
import {Loadingservice} from '../servives/loading';

const CUSTOMERS_URL = "http://localhost:8080/CUSTOMER-SERVICE/customers";

@Component({
  imports: [
    AsyncPipe,
    ReactiveFormsModule
  ],
  selector: 'app-customers',
  styleUrl: './customers.css',
  templateUrl: './customers.html',
})

export class Customers {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  public loadService = inject(Loadingservice);

  // chaque next() recharge la liste (apres un ajout par exemple)
  private refresh = new BehaviorSubject<void>(undefined);

  customers: Observable<CustomerListState> = this.refresh.pipe(
    switchMap(() => this.http.get<Customer[]>(CUSTOMERS_URL).pipe(
      map(resp => {
        return {customers: resp, status: RequestStatus.SUCCES}
      }),
      catchError((err: HttpErrorResponse) => {
        return of({status: RequestStatus.ERROR, errorMessage: apiErrorMessage(err)})
      })
    ))
  );

  customerForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });
  formError = signal<string | null>(null);
  formSuccess = signal<string | null>(null);

  saveCustomer() {
    if (this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      return;
    }
    this.formError.set(null);
    this.formSuccess.set(null);
    this.http.post<Customer>(CUSTOMERS_URL, this.customerForm.getRawValue()).subscribe({
      next: customer => {
        this.formSuccess.set(`Client ${customer.name} cree (id ${customer.id})`);
        this.customerForm.reset();
        this.refresh.next();
      },
      error: (err: HttpErrorResponse) => this.formError.set(apiErrorMessage(err))
    });
  }

  protected readonly RequestStatus = RequestStatus;
}
