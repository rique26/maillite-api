package com.maillite.mailliteapi.message.controller;

import com.maillite.mailliteapi.message.dto.request.SendMessageRequestDto;
import com.maillite.mailliteapi.message.dto.response.MessageResponseDto;
import com.maillite.mailliteapi.message.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    /**
     * RF05, paginada. Exemplo: GET /v1/messages/inbox?page=0&size=20
     * Ordenação padrão: mais recentes primeiro (sentAt DESC) — pode ser sobrescrita
     * via ?sort=subject,asc, por exemplo.
     */
    @GetMapping("/inbox")
    @ResponseStatus(HttpStatus.OK)
    public Page<MessageResponseDto> inbox(
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return messageService.getInbox(pageable);
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