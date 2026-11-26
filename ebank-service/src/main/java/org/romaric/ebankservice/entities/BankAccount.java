package org.romaric.ebankservice.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.romaric.ebankservice.model.Customer;

import java.util.Date;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BankAccount {
    @Id
    private String id;
    private Date createdAt;

    @PositiveOrZero(message = "le solde ne peut pas etre negatif")
    private double balance;

    @NotBlank(message = "le type est obligatoire")
    @Pattern(regexp = "CURRENT|SAVING|BUSINESS", message = "le type doit etre CURRENT, SAVING ou BUSINESS")
    private String type;

    @Positive(message = "customerId est obligatoire et doit etre positif")
    private long customerId;

    @Transient
    private Customer customer;
}
