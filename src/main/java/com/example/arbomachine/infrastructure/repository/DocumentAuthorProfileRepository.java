package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Dane autora opracowania zawsze w kontekscie konta (1:1) - nigdy wyszukiwanie po samym id profilu. */
@Repository
public interface DocumentAuthorProfileRepository extends JpaRepository<DocumentAuthorProfile, Long> {

    Optional<DocumentAuthorProfile> findByUserAccountId(Long userAccountId);
}
