package com.microservice.balance_service.repository;

import com.microservice.balance_service.model.entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> {
}