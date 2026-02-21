package com.crafter.lol.service;

import com.crafter.lol.dto.GameQuestion;
import com.crafter.lol.dto.ItemOption;
import com.crafter.lol.dto.ValidationRequest;
import com.crafter.lol.dto.ValidationResponse;
import com.crafter.lol.model.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final ItemService itemService;
    private final Random random = new Random();

    private static final int EASY_OPTIONS = 6;
    private static final int MEDIUM_OPTIONS = 10;
    private static final int HARD_OPTIONS = 12;

    private static final int EASY_TIME = 50;
    private static final int MEDIUM_TIME = 40;  // Aumentado para debug
    private static final int HARD_TIME = 30;

    public GameQuestion generateQuestion(String difficulty) {
        difficulty = difficulty != null ? difficulty.toUpperCase() : "MEDIUM";

        Map<String, Item> craftableItems = itemService.getCraftableItems();

        if (craftableItems.isEmpty()) {
            throw new RuntimeException("No craftable items available");
        }

        // Seleccionar un item aleatorio como objetivo
        List<Item> itemList = new ArrayList<>(craftableItems.values());
        Item targetItem = itemList.get(random.nextInt(itemList.size()));

        log.info("Generated question for item: {} ({})", targetItem.getName(), targetItem.getId());

        // Obtener componentes correctos
        List<String> correctComponentIds = targetItem.getFrom();

        // NUEVO: Crear lista completa de componentes correctos con todos los datos
        Map<String, Item> allItems = itemService.getAllItems();
        List<ItemOption> correctComponents = correctComponentIds.stream()
                .map(allItems::get)
                .filter(Objects::nonNull)
                .map(item -> ItemOption.builder()
                        .itemId(item.getId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .cost(item.getTotalCost())
                        .build())
                .collect(Collectors.toList());

        log.info("Correct components for {}: {}", targetItem.getName(),
                correctComponents.stream()
                        .map(ItemOption::getName)
                        .collect(Collectors.joining(", ")));

        // Generar opciones (correctas + distractores)
        int totalOptions = getTotalOptions(difficulty);
        List<ItemOption> options = generateOptions(
                correctComponentIds,
                totalOptions,
                craftableItems
        );

        return GameQuestion.builder()
                .targetItemId(targetItem.getId())
                .targetItemName(targetItem.getName())
                .targetItemImageUrl(targetItem.getImageUrl())
                .correctComponentIds(correctComponentIds)
                .correctComponents(correctComponents)
                .options(options)
                .timeLimit(getTimeLimit(difficulty))
                .difficulty(difficulty)
                .build();
    }

    public ValidationResponse validateAnswer(ValidationRequest request) {
        boolean isCorrect = itemService.isValidCraftingCombination(
                request.getTargetItemId(),
                request.getSelectedComponentIds()
        );

        Optional<Item> targetItem = itemService.getItemById(request.getTargetItemId());

        if (targetItem.isEmpty()) {
            return ValidationResponse.builder()
                    .correct(false)
                    .message("Invalid target item")
                    .scorePoints(0)
                    .build();
        }

        List<String> correctIds = targetItem.get().getFrom();
        List<String> correctNames = correctIds.stream()
                .map(id -> itemService.getItemById(id))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getName())
                .collect(Collectors.toList());

        // NUEVO: Crear lista completa de componentes correctos con todos los datos
        Map<String, Item> allItems = itemService.getAllItems();
        List<ItemOption> correctComponents = correctIds.stream()
                .map(allItems::get)
                .filter(Objects::nonNull)
                .map(item -> ItemOption.builder()
                        .itemId(item.getId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .cost(item.getTotalCost())
                        .build())
                .collect(Collectors.toList());

        List<String> incorrectIds = request.getSelectedComponentIds().stream()
                .filter(id -> !correctIds.contains(id))
                .collect(Collectors.toList());

        int scorePoints = isCorrect ? calculateScore(correctIds.size()) : 0;
        String message = isCorrect ?
                "Â¡Correcto! +" + scorePoints + " puntos" :
                "Incorrecto. Los componentes correctos son: " + String.join(", ", correctNames);

        return ValidationResponse.builder()
                .correct(isCorrect)
                .correctComponentIds(correctIds)
                .correctComponentNames(correctNames)
                .correctComponents(correctComponents)
                .incorrectComponentIds(incorrectIds)
                .message(message)
                .scorePoints(scorePoints)
                .build();
    }

    private List<ItemOption> generateOptions(
            List<String> correctComponentIds,
            int totalOptions,
            Map<String, Item> allCraftableItems) {

        Set<ItemOption> options = new HashSet<>();
        Map<String, Item> allItems = itemService.getAllItems();

        // Agregar componentes correctos
        for (String componentId : correctComponentIds) {
            Item component = allItems.get(componentId);
            if (component != null) {
                options.add(ItemOption.builder()
                        .itemId(component.getId())
                        .name(component.getName())
                        .imageUrl(component.getImageUrl())
                        .cost(component.getTotalCost())
                        .build());
            }
        }

        // Agregar distractores (items que son componentes de otros items)
        List<Item> potentialDistractors = allItems.values().stream()
                .filter(item -> !correctComponentIds.contains(item.getId()))
                .filter(item -> item.getTotalCost() > 0)
                .collect(Collectors.toList());

        Collections.shuffle(potentialDistractors);

        int neededDistractors = totalOptions - options.size();
        for (int i = 0; i < Math.min(neededDistractors, potentialDistractors.size()); i++) {
            Item distractor = potentialDistractors.get(i);
            options.add(ItemOption.builder()
                    .itemId(distractor.getId())
                    .name(distractor.getName())
                    .imageUrl(distractor.getImageUrl())
                    .cost(distractor.getTotalCost())
                    .build());
        }

        // Convertir a lista y mezclar
        List<ItemOption> optionsList = new ArrayList<>(options);
        Collections.shuffle(optionsList);

        return optionsList;
    }

    private int getTotalOptions(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> EASY_OPTIONS;
            case "HARD" -> HARD_OPTIONS;
            default -> MEDIUM_OPTIONS;
        };
    }

    private int getTimeLimit(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> EASY_TIME;
            case "HARD" -> HARD_TIME;
            default -> MEDIUM_TIME;
        };
    }

    private int calculateScore(int componentCount) {
        // Puntos base segÃºn cantidad de componentes
        int baseScore = componentCount * 50;

        // Bonus por complejidad
        if (componentCount >= 3) {
            baseScore += 100;
        } else if (componentCount == 2) {
            baseScore += 50;
        }

        return baseScore;
    }
}