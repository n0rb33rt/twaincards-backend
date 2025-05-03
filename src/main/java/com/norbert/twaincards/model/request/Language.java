package com.norbert.twaincards.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Language {
  // Common languages
  ENGLISH("en"),
  UKRAINIAN("uk"),
  FRENCH("fr"),
  SPANISH("es"),
  GERMAN("de"),
  ITALIAN("it"),
  PORTUGUESE("pt"),
  RUSSIAN("ru"),
  JAPANESE("ja"),
  CHINESE_SIMPLIFIED("zh-Hans"),
  CHINESE_TRADITIONAL("zh-Hant"),
  KOREAN("ko"),
  ARABIC("ar"),

  // European languages
  ALBANIAN("sq"),
  BOSNIAN("bs"),
  BULGARIAN("bg"),
  CATALAN("ca"),
  CROATIAN("hr"),
  CZECH("cs"),
  DANISH("da"),
  DUTCH("nl"),
  ESTONIAN("et"),
  FINNISH("fi"),
  GREEK("el"),
  HUNGARIAN("hu"),
  ICELANDIC("is"),
  IRISH("ga"),
  LATVIAN("lv"),
  LITHUANIAN("lt"),
  MACEDONIAN("mk"),
  MALTESE("mt"),
  NORWEGIAN("nb"),
  POLISH("pl"),
  ROMANIAN("ro"),
  SERBIAN("sr"),
  SLOVAK("sk"),
  SLOVENIAN("sl"),
  SWEDISH("sv"),
  TURKISH("tr"),
  WELSH("cy"),

  // Asian languages
  BENGALI("bn"),
  HINDI("hi"),
  INDONESIAN("id"),
  KAZAKH("kk"),
  MALAY("ms"),
  PERSIAN("fa"),
  TAMIL("ta"),
  TELUGU("te"),
  THAI("th"),
  URDU("ur"),
  VIETNAMESE("vi"),

  // Other languages
  HEBREW("he"),
  SWAHILI("sw"),
  AFRIKAANS("af"),
  AMHARIC("am");

  private final String code;

  Language(String code) {
    this.code = code;
  }

  @JsonValue
  public String getCode() {
    return code;
  }

  @JsonCreator
  public static Language fromCode(String value) {
    if (value == null) {
      return null;
    }

    for (Language language : Language.values()) {
      if (language.code.equalsIgnoreCase(value)) {
        return language;
      }
    }

    try {
      return Language.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown language code or name: " + value);
    }
  }
}