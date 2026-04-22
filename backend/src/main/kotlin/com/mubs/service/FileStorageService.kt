package com.mubs.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import jakarta.annotation.PostConstruct
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

@Service
class FileStorageService(
    @Value("\${mubs.upload.photo-dir:./uploads/photos}") private val photoDir: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var uploadPath: Path

    @PostConstruct
    fun init() {
        uploadPath = Paths.get(photoDir).toAbsolutePath().normalize()
        Files.createDirectories(uploadPath)
        log.info("Photo upload directory: {}", uploadPath)
    }

    fun storePhoto(file: MultipartFile): String {
        val originalFilename = file.originalFilename ?: "photo"
        val extension = originalFilename.substringAfterLast(".", "jpg")
        val filename = "${UUID.randomUUID()}.$extension"
        val targetPath = uploadPath.resolve(filename)
        file.inputStream.use { input ->
            Files.copy(input, targetPath)
        }
        log.info("Stored photo: {}", filename)
        return filename
    }

    fun deletePhoto(photoUrl: String) {
        val filename = photoUrl.substringAfterLast("/")
        val targetPath = uploadPath.resolve(filename)
        try {
            Files.deleteIfExists(targetPath)
            log.info("Deleted photo: {}", filename)
        } catch (e: Exception) {
            log.warn("Failed to delete photo file: {}", filename, e)
        }
    }
}
