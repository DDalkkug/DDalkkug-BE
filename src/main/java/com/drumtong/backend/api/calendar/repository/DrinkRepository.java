package com.drumtong.backend.api.calendar.repository;

import com.drumtong.backend.api.calendar.entity.Drink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkRepository extends JpaRepository<Drink, Long> {
    Drink findByType(String type);
}