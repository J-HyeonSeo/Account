# 계좌 시스템 실습 프로젝트

---

## 프로젝트 설명

- 본 프로젝트는 실습 프로젝트입니다. 실무에서 사용될 용도로 제작되지 않았습니다.
- Spring-Boot 기반의 Back-End 프로젝트입니다.
- 계좌를 개설, 조회, 해지 할 수 있습니다.
- 계좌를 사용하여, 거래를 발생하고, 취소할 수 있습니다.
- 거래기록은 데이터베이스에 따로 저장이 됩니다.

## 계좌 API

- (POST) /account : 계좌를 개설합니다.
```json
(example)
Request Header :
{
    'Content-Type' : 'application/json'
}

Request Body : 
{
    'userId': 1,
    'initialBalance': 1000
}

Response Body :
{
    'userId': 1,
    'accountNumber': '1000000000',
    'registeredAt': '2023-01-01T09:00:00.1111111'
}
```

- (GET) /account?user_id= : user_id가 소유한 계좌들을 조회합니다.
```json
(example)

Response Body:
{
  'accountNumber': '1000000000', 
  'balance': 1000000
}
```
- (DELETE) /account : 계좌를 해지합니다.
```json
(example)

Request Header:
{
    'Content-Type': 'application/json'
}

Request Body:
{
    'userId': 1, 
    'accountNumber': '1000000000'
}

Response Body:
{
    'userId': 1
    'accountNumber': '1000000000'
    'unRegisteredAt': '2023-01-01T10:00:00.1111111'
}
```

## 거래 API

- (POST) /transaction/use : 거래를 발생시킵니다.
```json
(example)

Request Header:
{
    'Content-Type' : 'application/json'
}

Request Body:
{
    'userId' : 1, 
    'accountNumber' : "1000000000", 
    'amount' : 10
}

Response Body:
{
    'accountNumber': '1000000000', 
    'transactionResult': 'S', 
    'transactionId': '2beb54382316486f9daab24a45726dcd',
    'amount': 10, 
    'transactedAt': '2023-01-01T09:30:00.1111111'  
}
```
- (POST) /transaction/cancel : 발생된 거래를 취소합니다.
```json
Request Header:
{
    'Content-Type' : 'application/json'
}

Request Body:
{
    'transactionId' : '2beb54382316486f9daab24a45726dcd',
    'accountNumber' : "1000000000",
    'amount' : 10
}

Response Body:
{
    'accountNumber': '1000000000', 
    'transactionResult': 'S', 
    'transactionId': 'b3e32e2579184c8db4c1e224283b59c5',
    'amount': 10, 
    'transactedAt': '2023-01-01T09:31:00.1111111'  
}
```
- (GET) transaction/{id} : id에 해당되는 transaction을 조회합니다.
```json
Response Body:
{
    'transactionType': 'CANCEL', 
    'accountNumber': '1000000000', 
    'transactionResult': 'S', 
    'transactionId': 'b3e32e2579184c8db4c1e224283b59c5', 
    'amount': 10, 
    'transactedAt': '2023-06-25T08:34:16.572881'
}
```

## 기술 스택
| SKILLS                                                                                                  |
|---------------------------------------------------------------------------------------------------------|
| <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat&logo=springboot&logoColor=white"/> |
| <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white"/>            |

## 프로젝트 환경

- Spring boot : v.2.7.11
- JDK : Eclipse Temurin version 17.0.7
- Language Level : 11

## 테스트
- JUnit
- Mockito
- h2Database