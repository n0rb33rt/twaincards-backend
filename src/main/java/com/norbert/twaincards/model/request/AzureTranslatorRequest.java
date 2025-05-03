package com.norbert.twaincards.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AzureTranslatorRequest(
        @JsonProperty("to_translate")
        String toTranslate,
        @JsonProperty("from_language")
        Language fromLanguage,
        @JsonProperty("to_language")
        Language toLanguage
) {

}