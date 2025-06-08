package com.example.itemapi.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.itemapi.model.Item;
import com.example.itemapi.service.ItemService;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Item>> getItems(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(itemService.getItems(category));
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        return new ResponseEntity<>(itemService.createItem(item), HttpStatus.CREATED);
    }

    // -------------------- Async Endpoints --------------------
    @GetMapping("/by-id")
    public CompletableFuture<ResponseEntity<Item>> getItemAsync(@RequestParam Long id) {
        return itemService.getItemByIdAsync(id)
                .thenApply(item -> item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<Item>> createItemAsync(@RequestBody Item item) {
        return itemService.createItemAsync(item)
                .thenApply(saved -> saved != null ? ResponseEntity.status(HttpStatus.CREATED).body(saved)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/info")
    public CompletableFuture<ResponseEntity<String>> getCombinedItemInfo(@RequestParam Long id) {
        return itemService.getCombinedItemInfo(id)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/update")
    public CompletableFuture<ResponseEntity<Item>> updateItemAsync(@RequestParam Long id, @RequestBody Item updated) {
        return itemService.updateItemAsync(id, updated)
                .thenApply(result -> result != null ? ResponseEntity.ok(result)
                : ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public CompletableFuture<ResponseEntity<Void>> deleteItemAsync(@RequestParam Long id) {
        return itemService.deleteItemAsync(id)
                .thenApply(deleted -> deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public CompletableFuture<ResponseEntity<Item>> searchItemsAsync(@RequestParam String keyword) {
        return itemService.searchItemsAsync(keyword)
                .thenApply(ResponseEntity::ok);
    }
}
