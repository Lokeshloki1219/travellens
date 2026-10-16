package com.travellens.backend.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Stores traveler post media in a Cloud Storage bucket.
 *
 * Through Milestone 3 uploads were written to a directory inside the container,
 * backed by a Docker volume on the VM. That made the VM stateful: deleting it
 * destroyed every photo users had posted. The bytes now live in a bucket, so
 * the compute layer holds no user data at all.
 *
 * Authentication is Application Default Credentials. On the VM that resolves to
 * the attached service account via the instance metadata server, so there is no
 * key file anywhere in the image, the repository, or on disk. Nothing to leak
 * and nothing to rotate.
 *
 * The bucket has public access prevention enforced and uniform bucket-level
 * access, so objects are never reachable by URL. Media is served back through
 * MediaController, which reads the object with the same service-account
 * identity.
 */
@Service
public class StorageService {

    private final Storage storage;
    private final String bucket;

    public StorageService(@Value("${app.storage.bucket}") String bucket) {
        this.bucket = bucket;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Uploads one multipart file and returns the generated object name.
     * The name is a random UUID plus the original extension, so a user-supplied
     * filename can never control the object path.
     */
    public String upload(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                : "";
        String objectName = UUID.randomUUID() + extension;

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectName))
                .setContentType(contentType)
                .build();

        storage.createFrom(blobInfo, file.getInputStream());
        return objectName;
    }

    /** Returns the blob, or null when the object does not exist. */
    public Blob get(String objectName) {
        return storage.get(BlobId.of(bucket, objectName));
    }

    public String getBucket() {
        return bucket;
    }
}
