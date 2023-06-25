package com.example.jhsfully.account.service;

import com.example.jhsfully.account.domain.Account;
import com.example.jhsfully.account.domain.AccountUser;
import com.example.jhsfully.account.domain.Transaction;
import com.example.jhsfully.account.dto.TransactionDto;
import com.example.jhsfully.account.exeption.AccountException;
import com.example.jhsfully.account.repository.AccountRepository;
import com.example.jhsfully.account.repository.AccountUserRepository;
import com.example.jhsfully.account.repository.TransactionRepository;
import com.example.jhsfully.account.type.AccountStatus;
import com.example.jhsfully.account.type.ErrorCode;
import com.example.jhsfully.account.type.TransactionResultType;
import com.example.jhsfully.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.jhsfully.account.type.TransactionResultType.S;
import static com.example.jhsfully.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(
                                            12L,
                                            "1000000012",
                                            1500L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(S, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(8500L, captor.getValue().getBalanceSnapshot());
        assertEquals(1500L, captor.getValue().getAmount());
    }

    @Test
    @DisplayName("Not exist User -> Can't Use Balance")
    void useBalanceFailed_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class, () ->
                transactionService.useBalance(1L, "1000011001", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Not exist account -> Can't Use Balance")
    void useBalanceFailed_AccountNotFound(){
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
                transactionService.useBalance(1L, "1000000001", 10000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Not Matched Id -> Can't Use Balance")
    void useBalanceFailed_userUnMatched(){
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
                        .balance(1000L)
                        .accountNumber("1000000012").build()));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(user.getId(), "1000000012", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("Already unregistered -> Can't Use Balance")
    void useBalanceFailed_userAlreadyUnregistered(){
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
                () -> transactionService.useBalance(user.getId(), "1000000012", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Amount more bigger than Balance -> Can't Use Balance")
    void useBalanceFailed_exceedAmount(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(user.getId(), "1000000012", 1500L));
        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());

    }

    @Test
    @DisplayName("Save the Failed Transaction.")
    void saveFailedUseTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
//        given(accountUserRepository.findById(anyLong()))
//                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction(
                "1000000012",
                1500L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(TransactionResultType.F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(1500L, captor.getValue().getAmount());
    }

    @Test
    void successCancelBalance(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId",
                "1000000012",
                1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(S, captor.getValue().getTransactionResultType());
        assertEquals(TransactionType.CANCEL, captor.getValue().getTransactionType());
        assertEquals(10000L + 1000L, captor.getValue().getBalanceSnapshot());
        assertEquals(1000L, captor.getValue().getAmount());
        System.out.println("transactionId : " + captor.getValue().getTransactionId());
        System.out.println("test-transactionId : " + transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("Not exist account -> Can't Cancel Balance")
    void CancelBalanceFailed_Account_NotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000001",
                        10000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("Not exist transaction -> Can't Cancel Balance")
    void CancelBalanceFailed_Transaction_NotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000001",
                        10000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("unmatched transaction and account -> Can't Cancel Balance")
    void CancelBalanceFailed_Unmatched_transaction(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Account otherAccount = Account.builder()//No awaken Use Transaction.
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        otherAccount.setId(2L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(otherAccount));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000012",
                        1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }


    @Test
    @DisplayName("unmatched amounts -> Can't Cancel Balance")
    void CancelBalanceFailed_Unmatched_Amounts(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(10000L + 1000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000012",
                        900L));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }


    @Test
    @DisplayName("Too old transaction -> Can't Cancel Balance")
    void CancelBalanceFailed_Old_Transaction(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactedAt(LocalDateTime.now().minusYears(2))
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .amount(1000L)
                .balanceSnapshot(10000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000012",
                        1000L));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }


    @Test
    void successQueryTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .name("KANA").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactedAt(LocalDateTime.now().minusYears(2))
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .amount(1000L)
                .balanceSnapshot(10000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService
                .queryTransaction("transactionId");
        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(1000L, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }


    @Test
    @DisplayName("Not exist transaction -> Can't query transaction")
    void FailedQueryTransaction(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}