document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async function(event) {
            event.preventDefault(); // 폼 기본 제출 동작 방지

            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const messageArea = document.getElementById('messageArea');
            messageArea.textContent = ''; // 이전 메시지 초기화
            messageArea.className = 'message';

            // Spring Security의 formLogin은 기본적으로 'application/x-www-form-urlencoded' 형식을 기대합니다.
            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);

            try {
                const response = await fetch('/login', { // Spring Security의 기본 로그인 처리 URL
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: formData
                });

                if (response.url && response.url.includes('error=true')) { 
                    messageArea.textContent = '아이디 또는 비밀번호가 잘못되었습니다.';
                    messageArea.className = 'message error';
                } else if (response.ok) {
                    window.location.href = response.url; 
                } else {
                    messageArea.textContent = '로그인 처리 중 예상치 못한 오류가 발생했습니다. 상태: ' + response.status;
                    messageArea.className = 'message error';
                }
            } catch (error) {
                console.error('Login Fetch Error:', error);
                messageArea.textContent = '로그인 요청 중 네트워크 오류가 발생했습니다. 다시 시도해주세요.';
                messageArea.className = 'message error';
            }
        });
    }

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        const messageArea = document.getElementById('messageArea');
        if(messageArea) { // messageArea가 존재하는지 확인 후 접근
            messageArea.textContent = '아이디 또는 비밀번호가 잘못되었습니다.';
            messageArea.className = 'message error';
        }
    }
});