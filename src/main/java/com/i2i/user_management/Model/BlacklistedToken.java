package com.i2i.user_management.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
public class BlacklistedToken {

    @Id
    private String token;

    private LocalDateTime expiryTime;

    public BlacklistedToken() {}

    public BlacklistedToken(String token, LocalDateTime expiryTime) {
        this.token = token;
        this.expiryTime = expiryTime;
    }

}
