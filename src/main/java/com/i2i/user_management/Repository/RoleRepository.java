package com.i2i.user_management.Repository;

import com.i2i.user_management.Model.Role;
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
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE r.isDeleted = false")
    List<Role> findAllActive();

    @Query("SELECT r FROM Role r WHERE r.id = :id AND r.isDeleted = false")
    Optional<Role> findActiveById(@Param("id") UUID id);

    @Query("SELECT r FROM Role r WHERE r.name = :name AND r.isDeleted = false")
    Optional<Role> findActiveByName(@Param("name") String name);

    @Modifying
    @Query("UPDATE Role r SET r.isDeleted = true, r.updatedAt = :updatedAt WHERE r.id = :id")
    void softDelete(@Param("id") UUID id, @Param("updatedAt") LocalDateTime updatedAt);

}
