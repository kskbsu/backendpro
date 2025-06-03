package com.backend.project.service;

import com.backend.project.dto.ChatRoomDTO; // ChatRoomDTO import 추가
import com.backend.project.model.ChatRoom;
import com.backend.project.model.ChatRoomParticipant;
import com.backend.project.model.User;
import com.backend.project.repository.ChatRoomParticipantRepository;
import com.backend.project.repository.ChatRoomRepository;
import com.backend.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.security.core.userdetails.UsernameNotFoundException; // 현재 미사용
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final UserRepository userRepository;

    // `getOrCreateOneToOneChatRoom` 메소드 제거
    // `getChatRoomsForUser` 메소드 제거
    
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getAllChatRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        return chatRooms.stream()
                .map(ChatRoomDTO::fromEntity) // ChatRoom 엔티티를 ChatRoomDTO로 변환
                .collect(Collectors.toList());
    }
    
    // 여기에 다른 채팅방 관련 서비스 로직이 필요하면 추가할 수 있습니다.
    // 예를 들어, roomId로 채팅방 정보를 가져오는 메소드 등
}