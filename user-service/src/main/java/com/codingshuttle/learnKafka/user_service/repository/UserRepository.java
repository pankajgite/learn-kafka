package com.codingshuttle.learnKafka.user_service.repository;

import com.codingshuttle.learnKafka.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
