package com.travel.user.repository;

import com.travel.user.model.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserIdOrderByAcceptedAtDesc(UUID userId);

    @Query("SELECT COUNT(c) FROM UserConsent c WHERE c.userId = :userId AND c.consentType = :consentType")
    long countByUserIdAndConsentType(@Param("userId") UUID userId, @Param("consentType") String consentType);
}
