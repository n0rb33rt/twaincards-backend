package com.norbert.twaincards.exception;


public class TokenExpiredException extends RuntimeException{
  public TokenExpiredException(String message) {
    super(message);
  }
}