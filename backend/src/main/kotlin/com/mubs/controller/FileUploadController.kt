package com.mubs.controller

import com.mubs.service.FileStorageService
import com.mubs.service.TicketService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/tickets")
class FileUploadController(
    private val fileStorageService: FileStorageService,
    private val ticketService: TicketService
) {

    @PostMapping("/{id}/photos")
    fun uploadPhoto(
        @PathVariable id: String,
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val ticket = ticketService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val filename = fileStorageService.storePhoto(file)
        val photoUrl = "/uploads/photos/$filename"

        ticket.handlePhotos.add(photoUrl)
        ticketService.save(ticket)

        return ResponseEntity.ok(mapOf("url" to photoUrl, "filename" to filename))
    }

    @DeleteMapping("/{id}/photos")
    fun deletePhoto(
        @PathVariable id: String,
        @RequestParam("url") photoUrl: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val ticket = ticketService.findById(id)
            ?: return ResponseEntity.notFound().build()

        if (!ticket.handlePhotos.remove(photoUrl)) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Photo not found" as Any))
        }

        ticketService.save(ticket)
        fileStorageService.deletePhoto(photoUrl)

        return ResponseEntity.ok(mapOf("removed" to photoUrl as Any))
    }
}
