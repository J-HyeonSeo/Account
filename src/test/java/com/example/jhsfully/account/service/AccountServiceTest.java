package com.example.jhsfully.account.service;

import com.example.jhsfully.account.domain.Account;
import com.example.jhsfully.account.domain.AccountUser;
import com.example.jhsfully.account.dto.AccountDto;
import com.example.jhsfully.account.exeption.AccountException;
import com.example.jhsfully.account.repository.AccountRepository;
import com.example.jhsfully.account.repository.AccountUserRepository;
import com.example.jhsfully.account.type.AccountStatus;
import com.example.jhsfully.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    //================= Create Test ===============================
    @Test
    void createAccountSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                                .accountNumber("1000000012").build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                                .accountUser(user)
                                .accountNumber("1000000010").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000010").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("Not exist User -> Can't create Account")
    void createAccount_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("has max account is 10 each user!!")
    void createAccount_maxAccountIs10(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    //================= Delete Test ===============================
    @Test
    void deleteAccountSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));


        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "100000000");
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals("1000000012", accountDto.getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("Not exist User -> Can't terminate Account")
    void deleteAccountFailed_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Not exist account -> Can't terminate Account")
    void deleteAccountFailed_AccountNotFound(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class, () ->
                accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Not Matched Id -> Can't terminate Account")
    void deleteAccountFailed_userUnMatched(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        AccountUser otheruser = AccountUser.builder()
                .name("ALICE").build();
        otheruser.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(otheruser)
                        .balance(0L)
                        .accountNumber("1000000012").build()));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(user.getId(), "1000000012"));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("Balance is not empty -> Can't terminate Account")
    void deleteAccountFailed_NotEmptyBalance(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(100L)
                        .accountNumber("1000000012").build()));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(user.getId(), "1000000012"));
        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("Already unregistered -> Can't terminate Account")
    void deleteAccountFailed_userAlreadyUnregistered(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012").build()));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(user.getId(), "1000000012"));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountNumber("1111111111")
                        .accountUser(user)
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("1111111112")
                        .accountUser(user)
                        .balance(1200L)
                        .build(),
                Account.builder()
                        .accountNumber("1111111113")
                        .accountUser(user)
                        .balance(1300L)
                        .build()
        );

        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals("1111111112", accountDtos.get(1).getAccountNumber());
        assertEquals("1111111113", accountDtos.get(2).getAccountNumber());
        assertEquals(1000L, accountDtos.get(0).getBalance());
        assertEquals(1200L, accountDtos.get(1).getBalance());
        assertEquals(1300L, accountDtos.get(2).getBalance());
    }


    @Test
    void failedToGetAccounts(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}