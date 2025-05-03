package com.norbert.twaincards.util;

public class EmailConfirmationHtmlUtil {
  public static String getConfirmationHtml(String loginUrl) {
    return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>Account Confirmed - TwainCards</title>" +
            "    <style>" +
            "        * {" +
            "            margin: 0;" +
            "            padding: 0;" +
            "            box-sizing: border-box;" +
            "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;" +
            "        }" +
            "        " +
            "        body {" +
            "            background-color: #f7f9fc;" +
            "            color: #333;" +
            "            min-height: 100vh;" +
            "            display: flex;" +
            "            flex-direction: column;" +
            "            justify-content: center;" +
            "            align-items: center;" +
            "            padding: 2rem;" +
            "        }" +
            "        " +
            "        .confirmation-container {" +
            "            background-color: white;" +
            "            border-radius: 12px;" +
            "            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);" +
            "            padding: 3rem;" +
            "            max-width: 600px;" +
            "            width: 100%;" +
            "            text-align: center;" +
            "            animation: fadeIn 0.6s ease-in-out;" +
            "        }" +
            "        " +
            "        .logo {" +
            "            margin-bottom: 2rem;" +
            "            width: 120px;" +
            "            height: auto;" +
            "        }" +
            "        " +
            "        .checkmark-circle {" +
            "            width: 80px;" +
            "            height: 80px;" +
            "            position: relative;" +
            "            display: inline-block;" +
            "            margin-bottom: 1.5rem;" +
            "            animation: scaleIn 0.5s ease-in-out 0.3s both;" +
            "        }" +
            "        " +
            "        .checkmark-circle-bg {" +
            "            width: 100%;" +
            "            height: 100%;" +
            "            border-radius: 50%;" +
            "            background-color: #40c057;" +
            "            display: block;" +
            "        }" +
            "        " +
            "        .checkmark {" +
            "            width: 40px;" +
            "            height: 24px;" +
            "            border-bottom: 6px solid white;" +
            "            border-left: 6px solid white;" +
            "            display: block;" +
            "            position: absolute;" +
            "            top: 50%;" +
            "            left: 50%;" +
            "            transform: translate(-50%, -60%) rotate(-45deg);" +
            "            animation: checkmark 0.4s ease-in-out 0.8s both;" +
            "        }" +
            "        " +
            "        h1 {" +
            "            font-size: 1.8rem;" +
            "            color: #333;" +
            "            margin-bottom: 1rem;" +
            "        }" +
            "        " +
            "        p {" +
            "            font-size: 1.1rem;" +
            "            line-height: 1.6;" +
            "            color: #666;" +
            "            margin-bottom: 2rem;" +
            "        }" +
            "        " +
            "        .btn {" +
            "            display: inline-block;" +
            "            background-color: #228be6;" +
            "            color: white;" +
            "            text-decoration: none;" +
            "            padding: 0.8rem 2rem;" +
            "            border-radius: 6px;" +
            "            font-weight: 600;" +
            "            transition: background-color 0.2s ease;" +
            "            box-shadow: 0 4px 6px rgba(34, 139, 230, 0.2);" +
            "        }" +
            "        " +
            "        .btn:hover {" +
            "            background-color: #1c7ed6;" +
            "        }" +
            "        " +
            "        .footer {" +
            "            margin-top: 3rem;" +
            "            font-size: 0.9rem;" +
            "            color: #888;" +
            "        }" +
            "        " +
            "        @keyframes fadeIn {" +
            "            from { opacity: 0; transform: translateY(20px); }" +
            "            to { opacity: 1; transform: translateY(0); }" +
            "        }" +
            "        " +
            "        @keyframes scaleIn {" +
            "            from { transform: scale(0); }" +
            "            to { transform: scale(1); }" +
            "        }" +
            "        " +
            "        @keyframes checkmark {" +
            "            0% { width: 0; height: 0; opacity: 0; }" +
            "            100% { width: 40px; height: 24px; opacity: 1; }" +
            "        }" +
            "        " +
            "        @media (max-width: 480px) {" +
            "            .confirmation-container {" +
            "                padding: 2rem;" +
            "            }" +
            "            " +
            "            h1 {" +
            "                font-size: 1.5rem;" +
            "            }" +
            "            " +
            "            p {" +
            "                font-size: 1rem;" +
            "            }" +
            "        }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"confirmation-container\">" +
            "        <div class=\"checkmark-circle\">" +
            "            <span class=\"checkmark-circle-bg\"></span>" +
            "            <span class=\"checkmark\"></span>" +
            "        </div>" +
            "        " +
            "        <h1>Account Successfully Confirmed</h1>" +
            "        <p>Thank you for registering with TwainCards! Your email has been verified and your account is now active. You can now sign in and start using all our features.</p>" +
            "        " +
            "        <a href=\"" + loginUrl + "\" class=\"btn\">Sign In</a>" +
            "        " +
            "        <div class=\"footer\">" +
            "            <p>If you have any questions, please contact our support team.</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
  }

}