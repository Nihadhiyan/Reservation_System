package com.bookfair.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.Role;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndActiveTrue(String email);

    Optional<User> findByUsernameAndActiveTrue(String username);

    Optional<User> findByIdAndActiveTrue(UUID id);

    boolean existsByEmailAndActiveTrue(String email);

    boolean existsByUsernameAndActiveTrue(String username);

    Optional<User> findByUsername(String username);

    Page<User> findAllByActiveTrue(Pageable pageable);

    long countByActiveTrue();

    long countByRoleAndActiveTrue(Role role);

    long countByOrganizationIdAndRoleAndActiveTrue(UUID id, Role orgAdmin);

    List<User> findAllByOrganizationId(UUID orgId);
    
    List<User> findByRole(Role role);

    void updateAllByOrganizationId(UUID orgId, Set<OrganizationCapability> newCapability);

}
