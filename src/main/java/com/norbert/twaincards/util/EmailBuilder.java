package com.norbert.twaincards.util;

import com.norbert.twaincards.model.request.BuildingEmailMessageRequest;

public class EmailBuilder {
  public static String buildEmailConfirmationMessage(BuildingEmailMessageRequest request){
    return String.format("""
            <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#333333;background-color:#f7f9fc;padding:30px">
              <div style="max-width:600px;margin:0 auto;background-color:#ffffff;border-radius:8px;box-shadow:0 4px 12px rgba(0,0,0,0.08);overflow:hidden">
                <!-- Header -->
                <div style="background-color:#228be6;padding:30px 0;text-align:center">
                  <h1 style="color:#ffffff;font-size:28px;margin:0;font-weight:600">Confirm Your Account</h1>
                </div>
                
                <!-- Content -->
                <div style="padding:40px 30px">
                  <p style="font-size:17px;line-height:1.6;margin-top:0;margin-bottom:25px">Hi %s,</p>
                  
                  <p style="font-size:17px;line-height:1.6;margin-bottom:25px">Thank you for registering with TwainCards! We're excited to have you join our community.</p>
                  
                  <p style="font-size:17px;line-height:1.6;margin-bottom:30px">Please confirm your email address to activate your account and start exploring all our features.</p>
                  
                  <div style="text-align:center;margin:40px 0">
                    <a href="%s" style="background-color:#228be6;color:#ffffff;font-size:16px;font-weight:600;text-decoration:none;padding:14px 40px;border-radius:6px;display:inline-block;box-shadow:0 4px 6px rgba(34,139,230,0.25)">Activate My Account</a>
                  </div>
                  
                  <p style="font-size:15px;line-height:1.6;margin-bottom:5px">This link will expire in 24 hours.</p>
                  
                  <p style="font-size:15px;line-height:1.6;margin-bottom:0">If you didn't create an account with us, please ignore this email.</p>
                </div>
                
                <!-- Button Link Fallback -->
                <div style="padding:0 30px 30px;font-size:14px;color:#666666">
                  <p style="margin-top:0;line-height:1.5;margin-bottom:15px">If the button above doesn't work, copy and paste this URL into your browser:</p>
                  <p style="margin:0;line-height:1.5;word-break:break-all;color:#228be6">%s</p>
                </div>
                
                <!-- Footer -->
                <div style="background-color:#f3f4f6;padding:30px;text-align:center;font-size:14px;color:#666666">
                  <p style="margin:0">If you have any questions, contact our support team.</p>
                </div>
              </div>
            </div>
            """, request.name(), request.link(), request.link());
  }
}