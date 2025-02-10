## 프로젝트 소개
[API 테스트 영상](https://www.youtube.com/watch?v=Y6SClkNVSwo)

## 시스템 아키텍처
![System_Architecture](https://github.com/user-attachments/assets/5a41693f-e9dd-4257-b34d-8de5457b0b49)

## ERD
![ERD](https://github.com/user-attachments/assets/9d2a2e7d-4c89-4baa-963d-eeccbfb2b7fe)

## 기술 스택
### Backend
![Java](https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white)
![Springboot](https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![JWT](https://img.shields.io/badge/jwt-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-FF4438?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### Tool
![IntelliJ IDEA](https://img.shields.io/badge/intellijidea-000000?style=for-the-badge&logo=intellijidea&logoColor=white)
![Gradle](https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Github](https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white)
![Discord](https://img.shields.io/badge/discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Notion](https://img.shields.io/badge/notion-000000?style=for-the-badge&logo=notion&logoColor=white)


## 컨벤션
### 브랜치 컨벤션
```
- main
    - 실제 배포 CI/CD용 branch
- develop
    - 개발 CI/CD용 branch
- feature
    - 기능 구현용 branch
    - 반드시 `develop`에서 뻗어나와 `develop`으로 `merge` 되어야한다.
- fix
    - 배포 전 기능 수정용 branch
- hotfix
    - 실제 배포 버전에서 발생한 버그 수정용 branch
```
### PR 컨벤션
```
[Issue_종류] 구현_내용 #이슈_번호

ex) [feature] 로그인 구현 #1
```
- Pull Request만 날리고, Approve는 reviewer가 한다.
- `develop` branch로의 `merge`는 1명 이상의 Approve가 필요함.

### 커밋 컨벤션
```
Issue_종류: 구현 내용

ex) feat: 로그인 구현
```
- Pull Request만 날리고, Approve는 reviewer가 한다.
- `develop` branch로의 `merge`는 1명 이상의 Approve가 필요함.

### 패키지 컨벤션
- Domain Driven Design(도메인 주도 설계)
```
- domain : 애플리케이션의 비지니스 로직을 포함
    - entity
        - api: 외부와의 요청을 처리(Controller)
        - dao: 데이터베이스와의 상호작용(Repository)
        - domain: 엔티티 객체
        - dto: 데이터 전송 객체(DTO)
        - service: 비지니스 로직 처리(Service)
        - converter: entity ↔ dto 변환
- global: 애플리케이션 전반에서 사용할 수 있는 공통 코드를 포함
- infra: 외부 시스템과 연동되는 코드를 포함
```