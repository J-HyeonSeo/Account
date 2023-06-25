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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.jhsfully.account.type.ErrorCode.*;
import static com.example.jhsfully.account.type.TransactionResultType.F;
import static com.example.jhsfully.account.type.TransactionResultType.S;
import static com.example.jhsfully.account.type.TransactionType.CANCEL;
import static com.example.jhsfully.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long userId,
                                     String accountNumber,
                                     Long amount) {
        /**
         * 사용자가 없는 경우,
         * 계좌가 없는 경우,
         * 사용자 아이디와 계좌 소유주가 다른 경우,
         * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
         * 거래금액이 너무 작거나 큰 경우 실패 응답
         */
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount); //유효성 검사 수행, 하위 코드들은 통과한 이후임.

        Long accountBalance = account.getBalance();
        account.useBalance(amount);

        return saveAndGetTransaction(USE, S, account, amount);
    }

    //거래 전에 유효성 검사를 수행함.
    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (amount > account.getBalance()) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));
        saveAndGetTransaction(USE, F, account, amount);
    }

    private TransactionDto saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account,
            Long amount) {
        return TransactionDto.fromEntity(transactionRepository.save(
                        Transaction.builder()
                                .transactionType(transactionType)
                                .transactionResultType(transactionResultType)
                                .account(account)
                                .amount(amount)
                                .balanceSnapshot(account.getBalance())
                                .transactionId(UUID.randomUUID().toString().replace("-", ""))
                                .transactedAt(LocalDateTime.now())
                                .build()
                )
        );
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId,
                                        String accountNumber,
                                        Long amount) {

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);
        account.cancelBalance(amount);

        return saveAndGetTransaction(CANCEL, S, account, amount);
    }

    private void validateCancelBalance(Transaction transaction,
                                       Account account,
                                       Long amount) {

        if(!Objects.equals(transaction.getAccount().getId(), account.getId())){
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if(!Objects.equals(transaction.getAmount(), amount)){
            throw new AccountException(CANCEL_MUST_FULLY);
        }
        if(transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))){
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));
        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
    }
}
