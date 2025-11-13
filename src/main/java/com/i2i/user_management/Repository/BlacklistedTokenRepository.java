package com.i2i.user_management.Repository;

import com.i2i.user_management.Model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
}
