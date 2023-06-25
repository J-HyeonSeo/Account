package com.example.jhsfully.account.controller;

import com.example.jhsfully.account.domain.Account;
import com.example.jhsfully.account.dto.AccountDto;
import com.example.jhsfully.account.dto.CreateAccount;
import com.example.jhsfully.account.dto.DeleteAccount;
import com.example.jhsfully.account.exeption.AccountException;
import com.example.jhsfully.account.service.AccountService;
import com.example.jhsfully.account.type.AccountStatus;
import com.example.jhsfully.account.type.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAccount.Request(1L, 100L)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }


    @Test
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1000000001")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(1L, "1000000001")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1000000001"))
                .andDo(print());
    }


    @Test
    void successGetAccount() throws Exception {
        //given
        given(accountService.getAccount(anyLong()))
                .willReturn(Account.builder()
                        .accountNumber("3456")
                        .accountStatus(AccountStatus.IN_USE)
                        .build());
        //when
        //then
        mockMvc.perform(get("/account/117"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("3456"))
                .andExpect(jsonPath("$.accountStatus").value("IN_USE"))
                .andExpect(status().isOk());
    }

    @Test
    void successGetAccountsByUserId() throws Exception {
        //given
        List<AccountDto> accountDtos =
                Arrays.asList(AccountDto.builder()
                                .accountNumber("1000000000")
                                .balance(1000L)
                                .build(),
                        AccountDto.builder()
                                .accountNumber("1000000021")
                                .balance(2300L)
                                .build(),
                        AccountDto.builder()
                                .accountNumber("1005890000")
                                .balance(1778778L)
                                .build());
        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);
        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1000000000"))
                .andExpect(jsonPath("$[0].balance").value(1000L))
                .andExpect(jsonPath("$[1].accountNumber").value("1000000021"))
                .andExpect(jsonPath("$[1].balance").value(2300L))
                .andExpect(jsonPath("$[2].accountNumber").value("1005890000"))
                .andExpect(jsonPath("$[2].balance").value(1778778L))
                .andExpect(status().isOk());
    }

    @Test
    void failGetAccount() throws Exception {
        //given
        given(accountService.getAccount(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND,
            ErrorCode.ACCOUNT_NOT_FOUND.getDescription()));
        //when
        //then
        mockMvc.perform(get("/account/117"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value(ErrorCode.ACCOUNT_NOT_FOUND.getDescription()))
                .andExpect(status().isOk());
    }
}