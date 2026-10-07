package com.example.app.link;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Increments inside the database in one statement, so concurrent visits never overwrite each
     * other (a read-modify-write in Java would lose updates). The expiry check is part of the same
     * statement, so a link can't expire between a check and the count. Returns 0 if unknown or expired.
     * clearAutomatically drops stale entities so a later read in the same transaction sees the new count.
     */
    @Modifying(clearAutomatically = true)
    @Query("update ShortLink l set l.visitCount = l.visitCount + 1 "
            + "where l.code = :code and (l.expiresAt is null or l.expiresAt > :now)")
    int incrementVisitCount(@Param("code") String code, @Param("now") Instant now);
}
