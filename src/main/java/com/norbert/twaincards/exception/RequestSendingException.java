package com.norbert.twaincards.exception;

public class RequestSendingException extends RuntimeException{
  public RequestSendingException(String message) {
    super(message);
  }
}