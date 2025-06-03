// 전역 변수
var stompClient = null;
var currentRoomId = null;
var currentUserNickname = "Guest"; // 기본값, 로그인 시 변경됨
var currentUserPreferredLanguage = null; // 사용자의 선호 언어

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
    chatArea.style.display = connected ? 'block' : 'none';
    if (!connected) {
        conversation.innerHTML = ''; 
        currentRoomId = null;
        currentRoomDisplay.textContent = '';
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
        disconnect(true); 
    }
    
    currentRoomId = roomIdToConnect; 
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
        };
        stompClient.send("/app/chat.sendMessage/" + currentRoomId, {}, JSON.stringify(chatMessage));
        messageInput.value = ''; 
    }
}

function showMessage(message) {
    const messageElement = document.createElement('li'); 
    messageElement.classList.add('message-item'); 

    let messageText = "";
    // 서버에서 오는 BroadcastMessage DTO의 type 필드 사용
    if (message.type === 'JOIN') { 
        messageText = message.sender + ' joined!';
        messageElement.classList.add('event-message', 'join-message');
    } else if (message.type === 'LEAVE') { 
        messageText = message.sender + ' left!';
        messageElement.classList.add('event-message', 'leave-message');
    } else if (message.type === 'CHAT') { // CHAT
        messageText = `<strong>${message.sender}:</strong> ${message.content}`;
        messageElement.classList.add('chat-message');
        if (message.sender === currentUserNickname) {
            messageElement.classList.add('my-message');
        } else {
            messageElement.classList.add('other-message');
        }
        // 타임스탬프 추가 (간단하게)
        if(message.timestamp) {
            const time = new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            messageText += ` <small class="text-muted">(${time})</small>`;
        }
        // 원본 언어 표시 (사용자 선호 언어와 다를 경우)
        if (message.originalLanguage && currentUserPreferredLanguage && message.originalLanguage.toLowerCase() !== currentUserPreferredLanguage.toLowerCase()) {
            messageText += ` <small class="text-info" style="font-size: 0.8em;">(Original: ${message.originalLanguage})</small>`;
        }

    } else if (message.type === 'HISTORY') {
        // HISTORY 타입은 handleHistoryMessage에서 별도 처리하므로 여기서는 무시
        return; 
    }
    messageElement.innerHTML = messageText;
    conversation.appendChild(messageElement);
    conversation.scrollTop = conversation.scrollHeight; 
}

function handleHistoryMessage(historyMessage) {
    if (historyMessage.type === 'HISTORY' && historyMessage.history) {
        conversation.innerHTML = ''; // 기존 대화 내용 초기화
        historyMessage.history.forEach(msg => {
            // ChatMessage 엔티티 형식을 BroadcastMessage 형식으로 변환하여 showMessage 재활용
            // 이제 msg는 ChatMessageDTO 객체입니다. 필드명은 동일하게 사용 가능.
            const displayMsg = {
                sender: msg.senderNickname,
                content: msg.content,
                type: 'CHAT', // 히스토리 메시지도 CHAT 타입으로 표시
                timestamp: msg.timestamp, // ChatMessage 엔티티의 timestamp 사용
                originalLanguage: msg.originalLanguage // 원본 언어 추가
            };
            showMessage(displayMsg);
        });
        // 모든 히스토리 메시지 추가 후 스크롤 맨 아래로
        conversation.scrollTop = conversation.scrollHeight;
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