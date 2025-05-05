package com.norbert.twaincards.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norbert.twaincards.exception.RequestSendingException;
import com.norbert.twaincards.model.request.AzureTranslatorRequest;
import com.norbert.twaincards.model.response.AzureTranslatorResponse;
import com.norbert.twaincards.model.response.TranslatorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.eledge.urlbuilder.UrlBuilder;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslatorService {
  private final ObjectMapper objectMapper;
  private final OkHttpClient okHttpClient;
  private final CommonWordService commonWordService;

  @Value("${azure.translator.api.url}")
  private String url;

  @Value("${azure.translator.api.location}")
  private String location;

  @Value("${azure.translator.api.key}")
  private String key;

  public TranslatorResponse translate(AzureTranslatorRequest translatorRequest) {
    String sourceText = translatorRequest.toTranslate().trim();
    String sourceLanguageCode = translatorRequest.fromLanguage().getCode();
    String targetLanguageCode = translatorRequest.toLanguage().getCode();
    

    Optional<String> commonTranslation = commonWordService.findTranslation(
        sourceText, sourceLanguageCode, targetLanguageCode);
    
    if (commonTranslation.isPresent()) {
      log.info("Translation found in common words database: {}", sourceText);
      return new TranslatorResponse(commonTranslation.get());
    }
    
    log.info("Translation not found in common words database, calling Azure API: {}", sourceText);
    

    try {
      RequestBody body = buildRequestBody(translatorRequest);
      String requestUrl = buildUrl(translatorRequest);
      Request request = buildRequest(requestUrl, body);

      try (Response response = okHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new RequestSendingException("API returned error: " + response.code() + " " + response.message());
        }

        String responseBody = response.body().string();

        AzureTranslatorResponse[] responses = objectMapper.readValue(responseBody, AzureTranslatorResponse[].class);

        if (responses.length == 0 || responses[0].getTranslations() == null || responses[0].getTranslations().isEmpty()) {
          throw new RequestSendingException("Empty translation response from API");
        }

        String translatedText = responses[0].getTranslations().get(0).getText();
        
        return new TranslatorResponse(translatedText);
      }
    } catch (IOException e) {
      log.error("Translation request failed", e);
      throw new RequestSendingException("Exception during sending the request: " + e.getMessage());
    }
  }

  private String buildUrl(AzureTranslatorRequest request) {
    UrlBuilder urlBuilder = new UrlBuilder(url);
    urlBuilder.addParam("api-version", "3.0");
    urlBuilder.addParam("from", request.fromLanguage().getCode());
    urlBuilder.addParam("to", request.toLanguage().getCode());
    return urlBuilder.toString();
  }

  private RequestBody buildRequestBody(AzureTranslatorRequest request) {
    MediaType mediaType = MediaType.parse("application/json");
    String jsonBody = "[{\"Text\": \"" + request.toTranslate() + "\"}]";
    log.debug("Request body: {}", jsonBody);
    return RequestBody.create(mediaType, jsonBody);
  }

  private Request buildRequest(String url, RequestBody requestBody) {
    return new Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Ocp-Apim-Subscription-Key", key)
            .addHeader("Ocp-Apim-Subscription-Region", location)
            .addHeader("Content-type", "application/json")
            .build();
  }
}