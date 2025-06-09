package com.example.itemapi.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.itemapi.model.Item;
import com.example.itemapi.repository.ItemRepository;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // --- Blocking operations ---
    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public List<Item> getItems(String category) {
        return category != null
                ? itemRepository.findByCategory(category)
                : itemRepository.findAll();
    }

    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

    // --- Async operations with real logic ---
    @Async("customAsyncExecutor")
    public CompletableFuture<Item> getItemByIdAsync(Long id) {
        return CompletableFuture
                .supplyAsync(() -> itemRepository.findById(id).orElse(null))
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Async getItemById failed: {}", ex.getMessage());
                    return null;
                });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<List<Item>> searchItemsAsync(String keyword) {
        return CompletableFuture
                .supplyAsync(()
                        -> // ✅ Streaming logic: fetch all, then filter with Java Streams:
                        itemRepository.findAll().stream()
                        .filter(item
                                -> item.getName().toLowerCase().contains(keyword.toLowerCase())
                        || item.getCategory().toLowerCase().contains(keyword.toLowerCase())
                        )
                        .toList()
                )
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Async searchItems failed: {}", ex.getMessage());
                    return List.of();
                });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Item> createItemAsync(Item item) {
        return CompletableFuture
                .supplyAsync(() -> itemRepository.save(item))
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Async createItem failed: {}", ex.getMessage());
                    return null;
                });
    }
    
    @Async("customAsyncExecutor")
    public CompletableFuture<Item> updateItemAsync(Long id, Item updated) {
        return CompletableFuture
                .supplyAsync(() -> {
                    Optional<Item> existing = itemRepository.findById(id);
                    if (existing.isEmpty()) {
                        return null;  
                    }
                    Item item = existing.get();
                    item.setName(updated.getName());
                    item.setCategory(updated.getCategory());
                    return itemRepository.save(item);
                })
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Async updateItem failed: {}", ex.getMessage());
                    return null;
                });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Boolean> deleteItemAsync(Long id) {
        return CompletableFuture
                .supplyAsync(() -> {
                    boolean exists = itemRepository.existsById(id);
                    if (exists) {
                        itemRepository.deleteById(id);
                    }
                    return exists;
                })
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Async deleteItem failed: {}", ex.getMessage());
                    return false;
                });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<String> getCombinedItemInfo(Long id) {
        CompletableFuture<Item> itemFuture = getItemByIdAsync(id);
        CompletableFuture<List<Item>> relatedFuture = searchItemsAsync("DEFAULT_KEYWORD");

        return itemFuture
                .thenCombine(relatedFuture, (item, list) -> {
                    if (item == null) {
                        return "Item not found";
                    }
                    return "Item: " + item.getName() + " (related found: " + list.size() + ")";
                })
                .exceptionally(ex -> {
                    logger.error("Async getCombinedItemInfo failed: {}", ex.getMessage());
                    return "Error fetching combined info";
                });
    }
}
