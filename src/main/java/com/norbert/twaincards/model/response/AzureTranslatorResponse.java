package com.norbert.twaincards.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureTranslatorResponse {

  @JsonProperty("detectedLanguage")
  private DetectedLanguage detectedLanguage;

  @JsonProperty("translations")
  private List<Translation> translations;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DetectedLanguage {

    @JsonProperty("language")
    private String language;

    @JsonProperty("score")
    private Double score;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Translation {

    @JsonProperty("text")
    private String text;

    @JsonProperty("to")
    private String to;
  }
}