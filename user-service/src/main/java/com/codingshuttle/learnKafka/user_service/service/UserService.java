package com.codingshuttle.learnKafka.user_service.service;


import com.codingshuttle.learnKafka.event.UserCreatedEvent;
import com.codingshuttle.learnKafka.user_service.dto.CreateUserRequestDto;
import com.codingshuttle.learnKafka.user_service.entity.User;
import com.codingshuttle.learnKafka.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${kafka.topic.user-created-topic}")
    private String KAFKA_USER_CREATED_TOPIC;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<Long, UserCreatedEvent> kafkaTemplate;

    public void createUser(CreateUserRequestDto createUserRequestDto) {

        User user = modelMapper.map(createUserRequestDto, User.class);
        User savedUser = userRepository.save(user);

        UserCreatedEvent userCreatedEvent = modelMapper.map(savedUser, UserCreatedEvent.class);
        kafkaTemplate.send(KAFKA_USER_CREATED_TOPIC, userCreatedEvent.getId(), userCreatedEvent);

    }

}
