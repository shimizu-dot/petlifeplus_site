package com.example.petlife.controller;

import com.example.petlife.dto.contact.ContactRequest;
import com.example.petlife.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> send(@Valid @RequestBody ContactRequest request) {
        contactService.send(request);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
