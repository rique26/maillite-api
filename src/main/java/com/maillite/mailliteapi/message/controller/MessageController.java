package com.maillite.mailliteapi.message.controller;

import com.maillite.mailliteapi.message.dto.request.SendMessageRequestDto;
import com.maillite.mailliteapi.message.dto.response.MessageResponseDto;
import com.maillite.mailliteapi.message.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponseDto send(@RequestBody @Valid SendMessageRequestDto dto) {
        return messageService.send(dto);
    }

    @GetMapping("/inbox")
    @ResponseStatus(HttpStatus.OK)
    public List<MessageResponseDto> inbox() {
        return messageService.getInbox();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponseDto getById(@PathVariable Long id) {
        return messageService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        messageService.delete(id);
    }
}