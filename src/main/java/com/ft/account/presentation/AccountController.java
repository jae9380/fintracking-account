package com.ft.account.presentation;

import com.ft.account.application.AccountService;
import com.ft.account.application.dto.AccountResult;
import com.ft.account.presentation.dto.AccountResponse;
import com.ft.account.presentation.dto.CreateAccountRequest;
import com.ft.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Account", description = "계좌 API")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성")
    @PostMapping
    public ApiResponse<AccountResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResult result = accountService.create(userId, request.toCommand());
        return ApiResponse.created(AccountResponse.from(result));
    }

    @Operation(summary = "계좌 목록 조회")
    @GetMapping
    public ApiResponse<List<AccountResponse>> findAll(
            @RequestHeader("X-User-Id") Long userId) {
        List<AccountResponse> responses = accountService.findAll(userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "계좌 상세 조회")
    @GetMapping("/{accountId}")
    public ApiResponse<AccountResponse> findById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long accountId) {
        AccountResult result = accountService.findById(userId, accountId);
        return ApiResponse.success(AccountResponse.from(result));
    }

    @Operation(summary = "계좌 삭제")
    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long accountId) {
        accountService.delete(userId, accountId);
        return ApiResponse.noContent();
    }
}
