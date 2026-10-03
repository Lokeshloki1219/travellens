package com.travellens.backend.repository;

import com.travellens.backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByDestinationIdOrderByCreatedAtDesc(Long destinationId);
    List<Post> findAllByOrderByCreatedAtDesc();
}
