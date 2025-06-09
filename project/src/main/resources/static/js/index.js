// 전역 변수
var stompClient = null;
var currentRoomId = null;
var currentUserNickname = "Guest"; // 기본값, 로그인 시 변경됨
var currentUserPreferredLanguage = null; // 사용자의 선호 언어
var bufferedCurrentUserJoinMessage = null; // 현재 사용자의 JOIN 메시지 임시 저장

// DOM 요소
const connectButton = document.getElementById('connect');
const disconnectButton = document.getElementById('disconnect');
const roomIdInput = document.getElementById('roomId');
const sendButton = document.getElementById('send');
const messageInput = document.getElementById('message');
const conversation = document.getElementById('conversation');
const chatArea = document.getElementById('chat-area');
const userNicknameDisplay = document.getElementById('user-nickname-display');
const currentRoomDisplay = document.getElementById('current-room-display');
// const roomListElement = document.getElementById('room-list'); // 채팅방 목록 제거


// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    fetchCurrentUserNickname();
    // fetchChatRooms(); // 채팅방 목록 불러오기 제거
    document.getElementById('connect-form').addEventListener('submit', function(event) {
        event.preventDefault();
        connect();
    });

    disconnectButton.addEventListener('click', function() {
        disconnect();
    });

    sendButton.addEventListener('click', function() {
        sendMessage();
    });
    messageInput.addEventListener('keypress', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });

    // 로그아웃 버튼 이벤트 리스너 추가
    const logoutBtn = document.getElementById('logoutButton');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logoutAndRedirect);
    }
});

function fetchCurrentUserNickname() {
    fetch('/api/auth/me')
        .then(response => {
            if (!response.ok) {
                throw new Error('Not authenticated or error fetching user info');
            }
            return response.json();
        })
        .then(data => {
            currentUserNickname = data.nickname || "Guest";
            userNicknameDisplay.textContent = currentUserNickname;
            currentUserPreferredLanguage = data.preferredLanguage; // 선호 언어 저장
            console.log("User Info:", currentUserNickname, currentUserPreferredLanguage);
        })
        .catch(error => {
            console.error('Error fetching user nickname:', error);
            userNicknameDisplay.textContent = "Guest (Error)";
        });
}

function setConnected(connected) {
    roomIdInput.disabled = connected;
    connectButton.disabled = connected;
    disconnectButton.disabled = !connected;
    // chatArea.style.display = connected ? 'block' : 'none'; // 기존 방식
    // Tailwind CSS 사용 시 예시:
    // chatArea.classList.toggle('hidden', !connected);
    // chatArea.classList.toggle('flex', connected); // chatArea가 flex 컨테이너일 경우
    if (connected) {
        chatArea.style.display = 'flex'; // 또는 'block', 프로젝트 스타일에 맞게
    } else {
        chatArea.style.display = 'none';
    }

    if (!connected) {
        conversation.innerHTML = '';
        currentRoomId = null;
        currentRoomDisplay.textContent = '';
        bufferedCurrentUserJoinMessage = null; // 연결 끊길 때 버퍼 초기화
    }
}

function connect() {
    const roomIdToConnect = roomIdInput.value.trim();
    if (!roomIdToConnect) {
        alert('Please enter a Room ID.');
        return;
    }
    if (!currentUserNickname || currentUserNickname === "Guest" || currentUserNickname.includes("Error")) {
        alert('Could not get user nickname. Please try logging in again.');
        return;
    }

    if (stompClient !== null && stompClient.connected) {
        disconnect(true); // 이전 연결이 있다면 조용히 해제
    }

    currentRoomId = roomIdToConnect;
    bufferedCurrentUserJoinMessage = null; // 새 방 연결 시 버퍼 초기화
    currentRoomDisplay.textContent = `Current Room: ${currentRoomId.substring(0,8)}...`;

    var socket = new SockJS('/ws-chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);

        stompClient.subscribe('/topic/room/' + currentRoomId, function (chatMessage) {
            showMessage(JSON.parse(chatMessage.body));
        });

        // 이전 대화 내용 구독 (사용자 특정 큐)
        stompClient.subscribe('/user/queue/room.history', function(message) {
            handleHistoryMessage(JSON.parse(message.body));
        });

        // 서버에 JOIN 메시지 전송
        stompClient.send("/app/chat.addUser/" + currentRoomId,
            {},
            JSON.stringify({sender: currentUserNickname, content: null }) // ClientMessagePayload 형식, content는 null 가능
        );
        // conversation.innerHTML = ''; // 새 방 연결 시 이전 대화 내용 초기화는 handleHistoryMessage에서 수행
    }, function(error) {
        console.error('STOMP connection error: ', error);
        alert('Could not connect to chat server. Please try again. ' + error);
        setConnected(false);
    });
}


function disconnect(silentDisconnect = false) {
    if (stompClient !== null && currentRoomId) {
        bufferedCurrentUserJoinMessage = null; // 연결 끊을 때 버퍼 초기화
        stompClient.send("/app/chat.leaveUser/" + currentRoomId,
            {},
            JSON.stringify({sender: currentUserNickname, content: null}) // ClientMessagePayload 형식
        );

        stompClient.disconnect(function() {
            console.log("Disconnected from room: " + currentRoomId);
            if (!silentDisconnect) {
                 setConnected(false);
            }
        });
    } else if (!silentDisconnect) {
         setConnected(false);
    }
}

function sendMessage() {
    const messageContent = messageInput.value.trim();

    if (messageContent && stompClient && currentRoomId) {
        const chatMessage = {
            // ClientMessagePayload DTO 형식에 맞게 전송
            content: messageContent
            // sender는 서버에서 인증 정보로 처리하므로 클라이언트에서 보낼 필요 없음 (CHAT 메시지의 경우)
        };
        stompClient.send("/app/chat.sendMessage/" + currentRoomId, {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
}

function appendMessageToConversation(messageDataForDisplay) {
    const messageElement = document.createElement('li');
    // Tailwind CSS를 사용한다고 가정하고, 직접 스타일 클래스 적용 또는 기존 마커 클래스 사용
    // 예시: messageElement.classList.add('p-2', 'rounded', 'break-words', 'mb-2', 'max-w-[85%]', 'sm:max-w-[70%]');
    messageElement.classList.add('message-item'); // 기존 방식 유지 시

    let messageText = "";

    if (messageDataForDisplay.type === 'JOIN') {
        messageText = messageDataForDisplay.sender + ' joined!';
        // Tailwind 예시: messageElement.classList.add('italic', 'text-gray-600', 'text-center', 'text-sm', 'bg-gray-100', 'py-1', 'my-1', 'text-green-600', 'mx-auto');
        messageElement.classList.add('event-message', 'join-message'); // 기존 방식
    } else if (messageDataForDisplay.type === 'LEAVE') {
        messageText = messageDataForDisplay.sender + ' left!';
        // Tailwind 예시: messageElement.classList.add('italic', 'text-gray-600', 'text-center', 'text-sm', 'bg-gray-100', 'py-1', 'my-1', 'text-red-600', 'mx-auto');
        messageElement.classList.add('event-message', 'leave-message'); // 기존 방식
    } else if (messageDataForDisplay.type === 'CHAT') {
        messageText = `<strong>${messageDataForDisplay.sender}:</strong> ${messageDataForDisplay.content}`;
        // Tailwind 예시:
        // if (messageDataForDisplay.sender === currentUserNickname) {
        //     messageElement.classList.add('bg-blue-500', 'text-white', 'ml-auto', 'text-right', 'rounded-lg', 'px-3', 'py-2');
        // } else {
        //     messageElement.classList.add('bg-gray-200', 'text-gray-800', 'mr-auto', 'text-left', 'rounded-lg', 'px-3', 'py-2');
        // }
        messageElement.classList.add('chat-message'); // 기존 방식
        if (messageDataForDisplay.sender === currentUserNickname) {
            messageElement.classList.add('my-message'); // 기존 방식
        } else {
            messageElement.classList.add('other-message'); // 기존 방식
        }

        if(messageDataForDisplay.timestamp) {
            const time = new Date(messageDataForDisplay.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            // Tailwind 예시: text-xs ${messageDataForDisplay.sender === currentUserNickname ? 'text-blue-200' : 'text-gray-500'} mt-1
            messageText += ` <small class="text-muted">(${time})</small>`; // 기존 방식
        }
        if (messageDataForDisplay.originalLanguage && currentUserPreferredLanguage && messageDataForDisplay.originalLanguage.toLowerCase() !== currentUserPreferredLanguage.toLowerCase()) {
            // Tailwind 예시: text-xs ${messageDataForDisplay.sender === currentUserNickname ? 'text-blue-300' : 'text-sky-600'} italic mt-1
            messageText += ` <small class="text-info" style="font-size: 0.8em;">(Original: ${messageDataForDisplay.originalLanguage})</small>`; // 기존 방식
        }
    }

    if (messageText) {
        messageElement.innerHTML = messageText;
        conversation.appendChild(messageElement);
        conversation.scrollTop = conversation.scrollHeight;
    }
}

function showMessage(message) { // STOMP로부터 받은 원본 메시지
    if (message.type === 'HISTORY') { // HISTORY 타입은 handleHistoryMessage에서 직접 처리
        return;
    }

    if (message.type === 'JOIN' && message.sender === currentUserNickname) {
        console.log("[showMessage] Buffering current user's JOIN message:", message);
        bufferedCurrentUserJoinMessage = message; // 현재 사용자의 JOIN 메시지인 경우 버퍼에 저장하고 바로 표시하지 않음
        return;
    }

    // 다른 모든 메시지 (다른 사용자의 JOIN/LEAVE, 모든 CHAT 메시지)는 즉시 표시
    appendMessageToConversation(message);
}

function handleHistoryMessage(historyMessage) {
    if (historyMessage.type === 'HISTORY' && historyMessage.history) {
        conversation.innerHTML = ''; // 기존 대화 내용 초기화
        historyMessage.history.forEach(msg => {
            // ChatMessageDTO 객체 (서버에서 ChatMessage 엔티티를 변환한 것)
            const displayMsg = {
                sender: msg.senderNickname,
                content: msg.content,
                type: 'CHAT', // 히스토리 메시지도 CHAT 타입으로 표시
                timestamp: msg.timestamp,
                originalLanguage: msg.originalLanguage
            };
            appendMessageToConversation(displayMsg); // append 함수 직접 호출
        });
        // 모든 히스토리 메시지 추가 후 스크롤 맨 아래로
        conversation.scrollTop = conversation.scrollHeight;

        // 이전 대화 내용 로드 후, 버퍼에 있던 현재 사용자의 JOIN 메시지가 있다면 표시
        if (bufferedCurrentUserJoinMessage) {
            console.log("[handleHistoryMessage] Displaying buffered current user's JOIN message after history.");
            appendMessageToConversation(bufferedCurrentUserJoinMessage);
            bufferedCurrentUserJoinMessage = null; // 버퍼 비우기
        }
    }
}

function logoutAndRedirect(event) {
    event.preventDefault(); // 기본 링크 동작 방지
    if (stompClient && stompClient.connected) {
        console.log("Disconnecting before logout...");
        disconnect(true); // silentDisconnect를 true로 하여 UI 변경 최소화
        // STOMP 연결 해제 메시지가 서버로 전송될 시간을 약간 줍니다.
        setTimeout(() => {
            window.location.href = '/logout';
        }, 250); // 필요에 따라 시간 조절
    } else {
        window.location.href = '/logout';
    }
}
