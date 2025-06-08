package com.example.itemapi.service;

import java.util.List;
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

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public List<Item> getItems(String category) {
        if (category != null) {
            return itemRepository.findByCategory(category);
        }
        return itemRepository.findAll();
    }

    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

// -------------------- Async Service Functions --------------------
// ----------------------------------------------
    // ✅ Async version with fail-safety and observability
    // - Prevents thread starvation by using a bounded executor
    // - Scales across cores via ForkJoinPool/custom thread pool
    // - Adds timeout and fallback to avoid long-hanging async ops
    // ----------------------------------------------
    @Async("customAsyncExecutor")
    public CompletableFuture<Item> getItemByIdAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulates I/O or latency-bound operation
                TimeUnit.MILLISECONDS.sleep(500);
                return itemRepository.findById(id).orElse(null);
            } catch (InterruptedException e) {
                // Always reset interrupt flag to prevent thread poisoning
                Thread.currentThread().interrupt();
                return null;
            }
        })
                // Set a strict timeout to prevent thread starvation at AWS-scale
                .orTimeout(2, TimeUnit.SECONDS)
                // Log and gracefully fallback instead of crashing the service
                .exceptionally(ex -> {
                    logger.error("Async failure: {}", ex.getMessage());
                    return null;
                });
    }

    // ------------------------------------------------------
    // ✅ Fan-out + fail-fast async orchestration
    // - Executes two async calls in parallel
    // - Prevents deadlocks with short-lived futures
    // - Catches failures and returns partial response instead
    // ------------------------------------------------------
    public CompletableFuture<String> getCombinedItemInfo(Long id) {
        CompletableFuture<Item> itemFuture = getItemByIdAsync(id);

        CompletableFuture<String> categoryFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate another async dependency
                TimeUnit.MILLISECONDS.sleep(300);
                return "default-category";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "error"; // Graceful fallback if this task fails
            }
        });

        return itemFuture
                // Combine both results after async completion
                .thenCombine(categoryFuture, (item, category) -> {
                    if (item == null) {
                        return "Item not found";
                    }
                    return "Item: " + item.getName() + ", Category: " + category;
                })
                // Fallback in case either call fails or times out
                .exceptionally(ex -> {
                    logger.error("Failed to fetch item info concurrently: {}", ex.getMessage());
                    return "Partial result due to error";
                });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Item> searchItemsAsync(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate a search operation with a delay
                TimeUnit.MILLISECONDS.sleep(300);
                String name = keyword.trim();
                logger.info("Searching for item with name: {}", name);
                // Use the repository to find items by name
                return itemRepository.findByName(name).stream().findFirst().orElse(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Search interrupted: {}", e.getMessage());
                return null; // Graceful fallback
            }
        });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Item> createItemAsync(Item item) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate delay for async operation
                TimeUnit.MILLISECONDS.sleep(200);
                logger.info("Creating item: {}", item.getName());
                return itemRepository.save(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Item creation interrupted: {}", e.getMessage());
                return null; // Graceful fallback
            }
        });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Item> updateItemAsync(Long id, Item updated) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Item existingItem = itemRepository.findById(id).orElse(null);
                if (existingItem != null) {
                    existingItem.setName(updated.getName());
                    existingItem.setCategory(updated.getCategory());
                    return itemRepository.save(existingItem);
                }
                return null; // Item not found
            } catch (Exception e) {
                // DO NOT need InterruptedException because we are not awaiting on any blocking I/O call above in the try-block
                logger.error("Error updating item: {}", e.getMessage());
                return null; // Graceful fallback
            }
        });
    }

    @Async("customAsyncExecutor")
    public CompletableFuture<Boolean> deleteItemAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (itemRepository.existsById(id)) {
                    itemRepository.deleteById(id);
                    return true; // Deletion successful
                }
            } catch (Exception e) {
                logger.error("Error deleting item: {}", e.getMessage());
            }
            return false; // Item not found or deletion failed
        });

    }
}
