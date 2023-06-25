package com.example.jhsfully.account.controller;

import com.example.jhsfully.account.dto.AccountDto;
import com.example.jhsfully.account.dto.CancelBalance;
import com.example.jhsfully.account.dto.TransactionDto;
import com.example.jhsfully.account.dto.UseBalance;
import com.example.jhsfully.account.service.TransactionService;
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

import static com.example.jhsfully.account.type.TransactionResultType.S;
import static com.example.jhsfully.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(
                anyLong(),
                anyString(),
                anyLong()
        )).willReturn(TransactionDto.builder()
                        .accountNumber("1000010001")
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionalId")
                        .transactionResultType(S)
                        .build());
        //when
        //then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UseBalance.Request(1L,
                                        "1000010001",
                                        300L)
                                )
                        )
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000010001"))
                .andExpect(jsonPath("$.amount").value(12345L))
                .andExpect(jsonPath("$.transactionResult").value("S"));


    }

    @Test
    void successCancelBalance() throws Exception {
        //given
        given(transactionService.cancelBalance(
                anyString(),
                anyString(),
                anyLong()
        )).willReturn(TransactionDto.builder()
                .accountNumber("1000010001")
                .transactedAt(LocalDateTime.now())
                .amount(10000L)
                .transactionId("transactionalId")
                .transactionResultType(S)
                .build());
        //when
        //then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                        new CancelBalance.Request("transactionalId",
                                                "1000010001",
                                                1000L)
                                )
                        )
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000010001"))
                .andExpect(jsonPath("$.amount").value(10000L))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionalId"));


    }

    @Test
    void successQueryTransaction() throws Exception {
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
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000010001")
                        .transactionType(USE)
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionalId")
                        .transactionResultType(S)
                        .build());
        //when
        //then
        mockMvc.perform(get("/transaction/transactionalId"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1000010001"))
                .andExpect(jsonPath("$.amount").value(12345L))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionalId"))
                .andExpect(jsonPath("$.transactionType").value("USE"));

    }
}