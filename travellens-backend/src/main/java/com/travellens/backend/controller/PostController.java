package com.travellens.backend.controller;

import com.travellens.backend.exception.ResourceNotFoundException;
import com.travellens.backend.model.Destination;
import com.travellens.backend.model.Post;
import com.travellens.backend.model.User;
import com.travellens.backend.repository.DestinationRepository;
import com.travellens.backend.repository.PostRepository;
import com.travellens.backend.repository.UserRepository;
import com.travellens.backend.storage.StorageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public PostController(PostRepository postRepository,
                          DestinationRepository destinationRepository,
                          UserRepository userRepository,
                          StorageService storageService) {
        this.postRepository = postRepository;
        this.destinationRepository = destinationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Post> getAll(@RequestParam(required = false) Long destinationId) {
        if (destinationId != null) {
            return postRepository.findByDestinationIdOrderByCreatedAtDesc(destinationId);
        }
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping(consumes = "multipart/form-data")
    public Post createPost(@RequestParam Long destinationId,
                           @RequestParam(required = false) String caption,
                           @RequestParam(required = false) String tip,
                           @RequestParam(required = false) Double approximateExpense,
                           @RequestParam(required = false) Integer rating,
                           @RequestParam(required = false) MultipartFile media,
                           Authentication authentication) throws IOException {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));

        Post post = new Post();
        post.setUser(user);
        post.setDestination(destination);
        post.setCaption(caption);
        post.setTip(tip);
        post.setApproximateExpense(approximateExpense);
        post.setRating(rating);

        if (media != null && !media.isEmpty()) {
            // Milestone 4: the bytes go to Cloud Storage rather than to a
            // directory on the container filesystem. The stored value keeps the
            // same /uploads/<name> shape, so rows written before and after the
            // move are read back identically.
            String objectName = storageService.upload(media);

            post.setMediaUrl("/uploads/" + objectName);
            String contentType = media.getContentType() == null ? "" : media.getContentType();
            post.setMediaType(contentType.startsWith("video") ? "VIDEO" : "IMAGE");
        }

        return postRepository.save(post);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        String email = authentication.getName();
        if (!post.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Post not found");
        }

        postRepository.deleteById(id);
    }
}
