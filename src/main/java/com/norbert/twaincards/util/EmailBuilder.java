package com.norbert.twaincards.util;

import com.norbert.twaincards.model.request.BuildingEmailMessageRequest;

public class EmailBuilder {
  public static String buildEmailConfirmationMessage(BuildingEmailMessageRequest request){
    return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; color: #333;\">" +
            "<div style=\"background-color: #5465ff; padding: 20px; color: white; text-align: center; border-radius: 8px 8px 0 0;\">" +
            "<h1 style=\"margin: 0; font-size: 24px;\">Welcome to TwainCards</h1>" +
            "</div>" +
            "<div style=\"background-color: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border: 1px solid #dee2e6; border-top: none;\">" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">Hi " + request.name() + ",</p>" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">Thank you for registering with TwainCards. Please confirm your email address to activate your account.</p>" +
            "<div style=\"text-align: center; margin: 30px 0;\">" +
            "<a href=\"" + request.link() + "\" style=\"background-color: #5465ff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Confirm Email</a>" +
            "</div>" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">If you didn't register for a TwainCards account, please ignore this email.</p>" +
            "<p style=\"font-size: 14px; color: #6c757d; margin-top: 30px; margin-bottom: 0;\">If the button doesn't work, copy and paste this URL into your browser:</p>" +
            "<p style=\"font-size: 14px; color: #6c757d; margin-top: 5px;\">" + request.link() + "</p>" +
            "</div>" +
            "<div style=\"text-align: center; padding: 20px; font-size: 12px; color: #6c757d;\">" +
            "<p>TwainCards - Learn languages with flashcards</p>" +
            "</div>" +
            "</div>";
  }

  public static String buildPasswordResetMessage(BuildingEmailMessageRequest request) {
    return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; color: #333;\">" +
            "<div style=\"background-color: #5465ff; padding: 20px; color: white; text-align: center; border-radius: 8px 8px 0 0;\">" +
            "<h1 style=\"margin: 0; font-size: 24px;\">Reset Your Password</h1>" +
            "</div>" +
            "<div style=\"background-color: #f8f9fa; padding: 20px; border-radius: 0 0 8px 8px; border: 1px solid #dee2e6; border-top: none;\">" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">Hi " + request.name() + ",</p>" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">We received a request to reset your password for your TwainCards account. Click the button below to create a new password.</p>" +
            "<div style=\"text-align: center; margin: 30px 0;\">" +
            "<a href=\"" + request.link() + "\" style=\"background-color: #5465ff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Reset Password</a>" +
            "</div>" +
            "<p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>" +
            "<p style=\"font-size: 14px; color: #6c757d; margin-top: 30px; margin-bottom: 0;\">If the button doesn't work, copy and paste this URL into your browser:</p>" +
            "<p style=\"font-size: 14px; color: #6c757d; margin-top: 5px;\">" + request.link() + "</p>" +
            "<p style=\"font-size: 14px; color: #6c757d; margin-top: 20px;\">This password reset link will expire in 24 hours.</p>" +
            "</div>" +
            "<div style=\"text-align: center; padding: 20px; font-size: 12px; color: #6c757d;\">" +
            "<p>TwainCards - Learn languages with flashcards</p>" +
            "</div>" +
            "</div>";
  }
}