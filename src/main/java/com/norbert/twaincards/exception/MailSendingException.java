package com.norbert.twaincards.exception;

public class MailSendingException extends RuntimeException{
  public MailSendingException(String message) {
    super(message);
  }
}