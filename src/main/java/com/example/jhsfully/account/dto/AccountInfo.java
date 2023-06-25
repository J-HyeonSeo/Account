package com.example.jhsfully.account.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {

    private String accountNumber;
    private Long balance;

}
