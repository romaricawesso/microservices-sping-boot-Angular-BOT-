package org.romaric.ebankservice.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Customer {

    private  Long id;
    private  String name;
    private  String email;
}
