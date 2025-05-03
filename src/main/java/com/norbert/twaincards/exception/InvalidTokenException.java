package com.norbert.twaincards.exception;

public class InvalidTokenException extends RuntimeException{
  public InvalidTokenException(String message) {
    super(message);
  }
}