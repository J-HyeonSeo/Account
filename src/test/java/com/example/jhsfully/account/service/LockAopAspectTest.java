package com.example.jhsfully.account.service;

import com.example.jhsfully.account.dto.UseBalance;
import com.example.jhsfully.account.exeption.AccountException;
import com.example.jhsfully.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;


    @Test
    void lockAndUnlock() throws Throwable {
        //given
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        UseBalance.Request request = new UseBalance.Request(1L,
                "1000010001", 1000L);
        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);
        //then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());

        assertEquals("1000010001", lockArgumentCaptor.getValue());
        assertEquals("1000010001", unlockArgumentCaptor.getValue());
    }

    @Test
    void lockAndUnlock_evenIfThrow() throws Throwable {
        //given
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        UseBalance.Request request = new UseBalance.Request(1L,
                "1000010001", 1000L);

        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        //when
        assertThrows(AccountException.class, () ->
            lockAopAspect.aroundMethod(proceedingJoinPoint, request));
        //then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());

        assertEquals("1000010001", lockArgumentCaptor.getValue());
        assertEquals("1000010001", unlockArgumentCaptor.getValue());
    }
}