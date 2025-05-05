package com.norbert.twaincards.controller;

import com.norbert.twaincards.service.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/confirm")
public class EmailConfirmationTokenController {
  private final TokenService tokenService;

  @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
  public String confirmAccount(@RequestParam("token") String token) {
    return tokenService.confirmAccount(token);
  }
}