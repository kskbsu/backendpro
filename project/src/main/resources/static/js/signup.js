document.addEventListener('DOMContentLoaded', function() {
    const signupForm = document.getElementById('signupForm');
    if (!signupForm) {
        return;
    }

    const FIELD_NAMES = ['username', 'password', 'nickname'];

    function clearFieldErrors() {
        FIELD_NAMES.forEach(function(name) {
            const el = document.getElementById(name + 'Error');
            if (el) {
                el.textContent = '';
            }
        });
    }

    function setFieldErrorsFromPayload(fieldErrors) {
        if (!Array.isArray(fieldErrors)) {
            return;
        }
        fieldErrors.forEach(function(item) {
            if (!item || typeof item.field !== 'string') {
                return;
            }
            const el = document.getElementById(item.field + 'Error');
            if (el) {
                el.textContent = item.message || '';
            }
        });
    }

    function resetMessageArea(messageArea) {
        messageArea.textContent = '';
        messageArea.classList.add('hidden');
        messageArea.className = 'hidden text-sm text-center rounded px-3 py-2 mb-4';
    }

    function showMessageArea(messageArea, kind, text) {
        messageArea.textContent = text;
        messageArea.classList.remove('hidden');
        var base = 'text-sm text-center rounded px-3 py-2 mb-4 ';
        messageArea.className = base + (kind === 'success'
            ? 'text-green-800 bg-green-50 border border-green-200'
            : 'text-red-800 bg-red-50 border border-red-200');
    }

    signupForm.addEventListener('submit', async function(event) {
        event.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const nickname = document.getElementById('nickname').value;
        const messageArea = document.getElementById('messageArea');
        const submitBtn = document.getElementById('signupSubmitBtn');

        if (!messageArea || !submitBtn) {
            console.error('messageArea or signupSubmitBtn not found');
            return;
        }

        clearFieldErrors();
        resetMessageArea(messageArea);
        submitBtn.disabled = true;

        var signupSucceeded = false;
        try {
            const response = await fetch('/api/auth/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username: username, password: password, nickname: nickname })
            });

            const contentType = response.headers.get('content-type') || '';
            var data = null;
            if (contentType.includes('application/json')) {
                data = await response.json();
            }

            if (response.ok) {
                signupSucceeded = true;
                var okMsg = (data && data.message) ? data.message : '회원가입 성공';
                showMessageArea(messageArea, 'success', okMsg);
                signupForm.reset();
                clearFieldErrors();
                setTimeout(function() {
                    window.location.href = '/login.html';
                }, 2000);
                return;
            }

            if (data) {
                setFieldErrorsFromPayload(data.fieldErrors);
                var errMsg = data.message || '요청을 처리할 수 없습니다.';
                showMessageArea(messageArea, 'error', errMsg);
            } else {
                var fallback = await response.text();
                showMessageArea(messageArea, 'error', fallback || '요청을 처리할 수 없습니다.');
            }
        } catch (error) {
            console.error('Error:', error);
            showMessageArea(messageArea, 'error', '회원가입 중 오류가 발생했습니다. 다시 시도해 주세요.');
        } finally {
            if (!signupSucceeded) {
                submitBtn.disabled = false;
            }
        }
    });
});
