package com.travellens.backend.controller;

import com.google.cloud.storage.Blob;
import com.travellens.backend.storage.StorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves traveler post media back out of the Cloud Storage bucket.
 *
 * This replaces the static resource handler that used to map /uploads/** onto a
 * directory on disk. Keeping the same URL shape matters: media_url values
 * already stored in the database still resolve, and the React frontend needed
 * no change at all.
 *
 * Reading through the application rather than exposing the bucket publicly is
 * deliberate. The bucket has public access prevention enforced, so the only way
 * to the bytes is this endpoint, using the VM service account's identity.
 */
@RestController
public class MediaController {

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/uploads/{objectName}")
    public ResponseEntity<byte[]> serve(@PathVariable String objectName) {
        // Reject anything that is not a plain object name, so a crafted path
        // cannot be used to reach outside the bucket prefix.
        if (objectName.contains("/") || objectName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Blob blob = storageService.get(objectName);
        if (blob == null || !blob.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = blob.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : blob.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(blob.getContent());
    }
}
