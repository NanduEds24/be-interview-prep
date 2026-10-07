package com.example.app.link;

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
     * other (a read-modify-write in Java would lose updates).
     */
    @Modifying
    @Query("update ShortLink l set l.visitCount = l.visitCount + 1 where l.code = :code")
    int incrementVisitCount(@Param("code") String code);
}
