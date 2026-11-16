# runners-auth

데브코스 부트캠프 인증 과제 실습 프로젝트입니다.

## 로컬 설정

실제 Spring 설정은 Git에서 제외되며 `application-example.yaml`만 공유합니다. 실제 비밀번호·JWT 서명 키·외부 서비스 시크릿은 환경변수로 관리하세요.

HTTP 테스트의 실제 값은 `src/test/http/http-client.private.env.json`의 `local`에 저장하고 요청 파일에서는 `{{testLoginPassword}}`, `{{testApiKey}}`처럼 참조하세요. IntelliJ 실행 환경은 `local`로 선택합니다. 공개 `http-client.env.json`에는 예제 값만 넣습니다.

## 커밋과 푸시

```bash
git add .
git commit -m "Update"
git push
```

현재 PC는 커밋·푸시 시 Secret check를 자동 실행합니다. 실제 설정·인증키·로그·덤프·IDE·빌드 파일은 차단합니다. 확인된 UUID 생성, 토큰 추출, null 검사, JJWT 의존성 등의 오탐은 구문을 한정해 허용합니다. 새 형태의 코드에는 오탐이 발생할 수 있으며 모든 비밀정보 탐지를 보장하지는 않습니다. BLOCKED가 나오면 우회하지 말고 원인을 확인하세요.

새로 복제한 환경은 Python 3와 Gitleaks 설치 후 `sh scripts/install-hooks.sh`를 실행해야 합니다.
