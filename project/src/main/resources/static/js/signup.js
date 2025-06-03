document.addEventListener('DOMContentLoaded', function() {
    const signupForm = document.getElementById('signupForm');
    if (signupForm) {
        signupForm.addEventListener('submit', async function(event) {
            event.preventDefault(); // 폼 기본 제출 동작 방지

            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const nickname = document.getElementById('nickname').value;
            const messageArea = document.getElementById('messageArea');

            if (!messageArea) {
                console.error('Message area not found');
                return;
            }
            messageArea.textContent = ''; // 이전 메시지 초기화
            messageArea.className = 'message';
            // signupForm.reset(); // 메시지 표시 전에 폼을 리셋할 필요는 없어 보입니다. 성공 시 리셋.

            try {
                const response = await fetch('/api/auth/signup', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username, password, nickname })
                });

                const responseText = await response.text(); // 서버 응답을 텍스트로 먼저 받음

                // AuthController에서는 성공 시 HttpStatus.CREATED (201)과 함께 "회원가입 성공" 메시지를 반환합니다.
                // response.ok는 상태 코드가 200-299 범위일 때 true입니다.
                if (response.ok) {
                    messageArea.textContent = responseText; // 서버에서 받은 성공 메시지
                    messageArea.className = 'message success';
                    signupForm.reset(); // 성공 시 폼 내용 초기화
                    setTimeout(() => {
                        window.location.href = '/login.html'; // login.html로 리디렉션 (기존 코드에서는 /login 이었음)
                    }, 2000); // 2초 후 이동
                } else {
                    messageArea.textContent = responseText; // 서버에서 받은 실패 메시지
                    messageArea.className = 'message error';
                }
            } catch (error) {
                console.error('Error:', error); // 콘솔 에러 메시지 일관성 유지
                messageArea.textContent = '회원가입 중 오류가 발생했습니다. 다시 시도해주세요.'; // 사용자에게 표시되는 메시지 일관성 유지
                messageArea.className = 'message error';
            }
        });
    }
});