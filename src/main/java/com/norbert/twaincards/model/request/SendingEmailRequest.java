package com.norbert.twaincards.model.request;

public record SendingEmailRequest(
        String message, String email,String subject
) {
}