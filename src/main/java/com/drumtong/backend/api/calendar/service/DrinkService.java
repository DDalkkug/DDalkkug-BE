package com.drumtong.backend.api.calendar.service;

import com.drumtong.backend.api.calendar.entity.Drink;
import com.drumtong.backend.api.calendar.repository.DrinkRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrinkService {
    private final DrinkRepository drinkRepository;

    @PostConstruct
    public void initDrinks() {
        // Initialize with our two standard drink types if they don't exist
        if (drinkRepository.findByType("소주") == null) {
            drinkRepository.save(Drink.builder()
                    .name("소주")
                    .type("소주")
                    .build());
        }

        if (drinkRepository.findByType("맥주") == null) {
            drinkRepository.save(Drink.builder()
                    .name("맥주")
                    .type("맥주")
                    .build());
        }
    }

    public Drink getDrinkByType(String type) {
        return drinkRepository.findByType(type);
    }
}