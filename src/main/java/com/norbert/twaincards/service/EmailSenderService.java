package com.norbert.twaincards.service;

import com.norbert.twaincards.exception.MailSendingException;
import com.norbert.twaincards.model.request.SendingEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@AllArgsConstructor
public class EmailSenderService {
  private final JavaMailSender mailSender;

  public void send(SendingEmailRequest request) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(request.message(), true);
      helper.setTo(request.email());
      helper.setSubject(request.subject());
      helper.setFrom("tradeflow.bot@gmail.com", "Twaincards");
      mailSender.send(mimeMessage);
    } catch (UnsupportedEncodingException | MessagingException e) {
      throw new MailSendingException("Failed to send email");
    }
  }
}
