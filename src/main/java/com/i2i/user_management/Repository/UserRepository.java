package com.i2i.user_management.Repository;

import com.i2i.user_management.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false and u.isActive = true")
    List<User> findAllActiveUsers();

    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true, u.updatedAt = :updatedAt, u.updatedBy = :deletedBy WHERE u.id = :id")
    void softDelete(@Param("id") UUID id, @Param("updatedAt") LocalDateTime updatedAt, @Param("deletedBy") String deletedBy);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isActive = true AND u.isDeleted = false")
    Optional<User> findActiveById(@Param("id") UUID id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true AND u.isDeleted = false")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email AND u.isActive = true AND u.isDeleted = false")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u.employeeId FROM User u WHERE u.employeeId LIKE CONCAT('i2i', :year, '%') ORDER BY u.employeeId DESC LIMIT 1")
    Optional<String> findLastEmployeeCodeForYear(@Param("year") String year);
}
