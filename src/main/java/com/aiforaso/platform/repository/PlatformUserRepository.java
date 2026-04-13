package com.aiforaso.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.PlatformUser;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {

    Optional<PlatformUser> findByEmailIgnoreCase(String email);
}
