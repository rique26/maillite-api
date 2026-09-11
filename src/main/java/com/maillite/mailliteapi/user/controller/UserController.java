package com.maillite.mailliteapi.user.controller;

import com.maillite.mailliteapi.user.dto.request.FcmTokenRequestDto;
import com.maillite.mailliteapi.user.dto.response.UserSummaryResponseDto;
import com.maillite.mailliteapi.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<UserSummaryResponseDto> search(@RequestParam("query") String query) {
        return userService.search(query);
    }

    @PostMapping("/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFcmToken(@RequestBody @Valid FcmTokenRequestDto dto) {
        userService.updateFcmToken(dto);
    }
}