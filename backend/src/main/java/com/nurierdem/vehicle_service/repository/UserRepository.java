package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    List<User> findAllByArchivedFalseOrderByFullNameAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findAccountForUpdate(@Param("id") Long id);
}
