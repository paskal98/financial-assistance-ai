package com.microservice.balance_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_balances")
@Data
public class UserBalance {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "updated_at")
    private Instant updatedAt;
}