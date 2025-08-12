package org.mulearn.protocol.controller;

import org.mulearn.protocol.domain.Message;
import org.mulearn.protocol.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    public String processMessage(@RequestBody Message message) {
        messageService.processMessage(message);
        return "Message received and is being processed.";
    }
}