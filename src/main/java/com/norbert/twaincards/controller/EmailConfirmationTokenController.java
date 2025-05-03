package com.norbert.twaincards.controller;

import com.norbert.twaincards.service.EmailConfirmationTokenService;
import com.norbert.twaincards.util.EmailConfirmationHtmlUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.norbert.twaincards.util.LocalConstants.FRONT_BASE_URL;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/confirm")
public class EmailConfirmationTokenController {
  private final EmailConfirmationTokenService emailConfirmationTokenService;

  @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
  public String confirmAccount(@RequestParam("token") String token) {
    emailConfirmationTokenService.confirmAccount(token);
    return EmailConfirmationHtmlUtil.getConfirmationHtml(FRONT_BASE_URL + "/login");
  }

}