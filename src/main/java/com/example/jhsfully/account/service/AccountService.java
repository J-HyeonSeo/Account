package com.example.jhsfully.account.service;

import com.example.jhsfully.account.domain.Account;
import com.example.jhsfully.account.domain.AccountUser;
import com.example.jhsfully.account.dto.AccountDto;
import com.example.jhsfully.account.exeption.AccountException;
import com.example.jhsfully.account.repository.AccountRepository;
import com.example.jhsfully.account.repository.AccountUserRepository;
import com.example.jhsfully.account.type.AccountStatus;
import com.example.jhsfully.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.jhsfully.account.type.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     *사용자가 있는지 조회
     *계좌 번호를 생성
     *계좌를 저장하고, 그 정보를 넘긴다.
     */
    @Transactional //계좌 생성
    public AccountDto createAccount(Long userId, Long initialBalance){

        //userId를 통해, AccountUser를 찾음.
        AccountUser accountUser = getAccountUser(userId);

        //밸리데이션
        validateCreateAccount(accountUser);

        //가장 큰 계좌 번호를 가져와 + 1
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        //Dto를 만들어 반환시킴.
        return AccountDto.fromEntity(
                accountRepository.save(
                Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(AccountStatus.IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()
                )
        );
    }

    //accountUser의 카운트를 세서 밸리데이션을 수행함, 조건에 만족하지 않으면 정의된 예외를 던지도록함.
    private void validateCreateAccount(AccountUser accountUser) {
        if(accountRepository.countByAccountUser(accountUser) >= 10){
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id){
        if(id < 0){
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    //계좌 해지 트랜잭션.
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        //userId를 통해, AccountUser를 찾음. 없을 경우 User not fount 예외를 던짐/
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if(!Objects.equals(accountUser.getId(), account.getAccountUser().getId())){
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if(account.getAccountStatus() == AccountStatus.UNREGISTERED){
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if(account.getBalance() > 0){
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        return accountUser;
    }
}
