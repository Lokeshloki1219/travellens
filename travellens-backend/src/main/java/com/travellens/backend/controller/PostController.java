package com.travellens.backend.controller;

import com.travellens.backend.exception.ResourceNotFoundException;
import com.travellens.backend.model.Destination;
import com.travellens.backend.model.Post;
import com.travellens.backend.model.User;
import com.travellens.backend.repository.DestinationRepository;
import com.travellens.backend.repository.PostRepository;
import com.travellens.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public PostController(PostRepository postRepository, DestinationRepository destinationRepository,
                           UserRepository userRepository) {
        this.postRepository = postRepository;
        this.destinationRepository = destinationRepository;
        this.userRepository = userRepository;
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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = media.getOriginalFilename() == null ? "file" : media.getOriginalFilename();
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String storedName = UUID.randomUUID() + extension;

            Path target = uploadPath.resolve(storedName);
            Files.copy(media.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            post.setMediaUrl("/uploads/" + storedName);
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
