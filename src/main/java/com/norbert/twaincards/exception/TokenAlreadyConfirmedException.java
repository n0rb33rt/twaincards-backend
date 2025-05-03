package com.norbert.twaincards.exception;

public class TokenAlreadyConfirmedException extends RuntimeException{
  public TokenAlreadyConfirmedException(String message) {
    super(message);
  }
}