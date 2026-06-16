package com.everytldr.common.domain.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everytldr.common.domain.source.SourcePolicy.RuleType;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SourcePolicyTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializesPolicyWithoutEligibilityWithDefaults() throws Exception {
    SourcePolicy policy =
        objectMapper.readValue(
            """
            {
              "crawling": {
                "feed_urls": ["https://globalvoices.org/feed/"],
                "hosts": ["globalvoices.org", "www.globalvoices.org"],
                "content_selectors": [".full-article .entry"],
                "thumbnail_selectors": []
              }
            }
            """,
            SourcePolicy.class);

    assertThat(policy.crawling().feedUrls()).containsExactly("https://globalvoices.org/feed/");
    assertThat(policy.eligibility().articleRules()).isEmpty();
    assertThat(policy.eligibility().hasArticleEligibilityRules()).isFalse();
    assertThat(policy.eligibility().thumbnailPolicy()).isEqualTo(ThumbnailPolicy.ALLOW);
    assertThat(policy.eligibility().thumbnailEligibility()).isNull();
  }

  @Test
  void deserializesPolicyWithArticleRulesAndThumbnailEligibility() throws Exception {
    SourcePolicy policy =
        objectMapper.readValue(
            """
            {
              "crawling": {
                "feed_urls": ["https://www.voanews.com/api/"],
                "hosts": ["www.voanews.com", "voanews.com"],
                "content_selectors": ["#article-content .wsw"],
                "thumbnail_selectors": []
              },
              "eligibility": {
                "article_rules": [
                  {
                    "type": "SELECTOR_EXISTS",
                    "selector": ".publishing-details .links__item-link"
                  },
                  {
                    "type": "SELECTOR_ATTRIBUTE_PREFIX_ANY",
                    "selector": ".publishing-details .links__item-link",
                    "attribute": "href",
                    "values": [
                      "/author/",
                      "https://www.voanews.com/author/",
                      "https://voanews.com/author/"
                    ]
                  },
                  {
                    "type": "DOCUMENT_HTML_NOT_CONTAINS_ANY",
                    "values": ["Reuters"]
                  }
                ],
                "thumbnail_policy": "ELIGIBLE_ONLY",
                "thumbnail_eligibility": {
                  "candidate_selectors": [
                    {
                      "selector": "figure img",
                      "url_attribute": "src",
                      "credit_container_selector": "figure",
                      "credit_selectors": ["figcaption", ".caption"]
                    }
                  ],
                  "allowed_credit_fragments": ["VOA", "Voice of America"],
                  "denied_credit_fragments": ["Reuters", "Associated Press"]
                }
              }
            }
            """,
            SourcePolicy.class);

    assertThat(policy.eligibility().articleRules())
        .extracting(SourcePolicy.ArticleEligibilityRule::type)
        .containsExactly(
            RuleType.SELECTOR_EXISTS,
            RuleType.SELECTOR_ATTRIBUTE_PREFIX_ANY,
            RuleType.DOCUMENT_HTML_NOT_CONTAINS_ANY);
    assertThat(policy.eligibility().articleRules().get(1).values())
        .containsExactly(
            "/author/", "https://www.voanews.com/author/", "https://voanews.com/author/");
    assertThat(policy.eligibility().thumbnailPolicy()).isEqualTo(ThumbnailPolicy.ELIGIBLE_ONLY);
    assertThat(policy.eligibility().thumbnailEligibility().candidateSelectors())
        .extracting(SourcePolicy.ThumbnailCandidateSelector::selector)
        .containsExactly("figure img");
    assertThat(policy.eligibility().thumbnailEligibility().allowedCreditFragments())
        .containsExactly("VOA", "Voice of America");
    assertThat(policy.eligibility().thumbnailEligibility().deniedCreditFragments())
        .containsExactly("Reuters", "Associated Press");
  }

  @Test
  void serializesPolicyWithoutDerivedEligibilityProperties() throws Exception {
    SourcePolicy policy =
        objectMapper.readValue(
            """
            {
              "crawling": {
                "feed_urls": ["https://www.voanews.com/api/"],
                "hosts": ["www.voanews.com", "voanews.com"],
                "content_selectors": ["#article-content .wsw"],
                "thumbnail_selectors": []
              },
              "eligibility": {
                "article_rules": [
                  {
                    "type": "DOCUMENT_HTML_NOT_CONTAINS_ANY",
                    "values": ["Reuters"]
                  }
                ],
                "thumbnail_policy": "DISABLED"
              }
            }
            """,
            SourcePolicy.class);

    String serialized = objectMapper.writeValueAsString(policy);

    assertThat(serialized).contains("\"article_rules\"");
    assertThat(serialized).contains("\"thumbnail_policy\":\"DISABLED\"");
    assertThat(serialized).doesNotContain("thumbnailDisabled");
    assertThat(serialized).doesNotContain("articleEligibilityRules");
  }

  @Test
  void trimsRuleAndThumbnailValues() throws Exception {
    SourcePolicy policy =
        objectMapper.readValue(
            """
            {
              "crawling": {
                "feed_urls": ["https://www.voanews.com/api/"],
                "hosts": ["www.voanews.com"],
                "content_selectors": ["#article-content .wsw"],
                "thumbnail_selectors": []
              },
              "eligibility": {
                "article_rules": [
                  {
                    "type": "SELECTOR_ATTRIBUTE_PREFIX_ANY",
                    "selector": " .publishing-details .links__item-link ",
                    "attribute": " href ",
                    "values": [" /author/ "]
                  }
                ],
                "thumbnail_policy": "ELIGIBLE_ONLY",
                "thumbnail_eligibility": {
                  "candidate_selectors": [
                    {
                      "selector": " figure img ",
                      "url_attribute": " src ",
                      "credit_container_selector": " figure ",
                      "credit_selectors": [" figcaption "]
                    }
                  ],
                  "allowed_credit_fragments": [" VOA "],
                  "denied_credit_fragments": [" Reuters "]
                }
              }
            }
            """,
            SourcePolicy.class);

    assertThat(policy.eligibility().articleRules().getFirst().selector())
        .isEqualTo(".publishing-details .links__item-link");
    assertThat(policy.eligibility().articleRules().getFirst().attribute()).isEqualTo("href");
    assertThat(policy.eligibility().articleRules().getFirst().values()).containsExactly("/author/");
    assertThat(
            policy.eligibility().thumbnailEligibility().candidateSelectors().getFirst().selector())
        .isEqualTo("figure img");
    assertThat(
            policy
                .eligibility()
                .thumbnailEligibility()
                .candidateSelectors()
                .getFirst()
                .creditSelectors())
        .containsExactly("figcaption");
    assertThat(policy.eligibility().thumbnailEligibility().allowedCreditFragments())
        .containsExactly("VOA");
    assertThat(policy.eligibility().thumbnailEligibility().deniedCreditFragments())
        .containsExactly("Reuters");
  }

  @Test
  void rejectsBlankRuleValues() {
    assertThatThrownBy(
            () ->
                objectMapper.readValue(
                    """
                    {
                      "crawling": {
                        "feed_urls": ["https://www.voanews.com/api/"],
                        "hosts": ["www.voanews.com"],
                        "content_selectors": ["#article-content .wsw"],
                        "thumbnail_selectors": []
                      },
                      "eligibility": {
                        "article_rules": [
                          {
                            "type": "DOCUMENT_HTML_NOT_CONTAINS_ANY",
                            "values": [" "]
                          }
                        ]
                      }
                    }
                    """,
                    SourcePolicy.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("values must not contain blank values");
  }

  @Test
  void rejectsRuleMissingRequiredSelector() {
    assertThatThrownBy(
            () ->
                objectMapper.readValue(
                    """
                    {
                      "crawling": {
                        "feed_urls": ["https://www.voanews.com/api/"],
                        "hosts": ["www.voanews.com"],
                        "content_selectors": ["#article-content .wsw"],
                        "thumbnail_selectors": []
                      },
                      "eligibility": {
                        "article_rules": [
                          {
                            "type": "SELECTOR_EXISTS"
                          }
                        ]
                      }
                    }
                    """,
                    SourcePolicy.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("selector must not be blank");
  }

  @Test
  void rejectsAttributeRuleMissingAttribute() {
    assertThatThrownBy(
            () ->
                objectMapper.readValue(
                    """
                    {
                      "crawling": {
                        "feed_urls": ["https://www.voanews.com/api/"],
                        "hosts": ["www.voanews.com"],
                        "content_selectors": ["#article-content .wsw"],
                        "thumbnail_selectors": []
                      },
                      "eligibility": {
                        "article_rules": [
                          {
                            "type": "SELECTOR_ATTRIBUTE_PREFIX_ANY",
                            "selector": ".author",
                            "values": ["/author/"]
                          }
                        ]
                      }
                    }
                    """,
                    SourcePolicy.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("attribute must not be blank");
  }

  @Test
  void rejectsEligibleOnlyThumbnailPolicyWithoutThumbnailEligibility() {
    assertThatThrownBy(
            () ->
                objectMapper.readValue(
                    """
                    {
                      "crawling": {
                        "feed_urls": ["https://www.voanews.com/api/"],
                        "hosts": ["www.voanews.com"],
                        "content_selectors": ["#article-content .wsw"],
                        "thumbnail_selectors": []
                      },
                      "eligibility": {
                        "thumbnail_policy": "ELIGIBLE_ONLY"
                      }
                    }
                    """,
                    SourcePolicy.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("thumbnailEligibility must be configured");
  }

  @Test
  void rejectsThumbnailCandidateMissingCreditSelectors() {
    assertThatThrownBy(
            () ->
                objectMapper.readValue(
                    """
                    {
                      "crawling": {
                        "feed_urls": ["https://www.voanews.com/api/"],
                        "hosts": ["www.voanews.com"],
                        "content_selectors": ["#article-content .wsw"],
                        "thumbnail_selectors": []
                      },
                      "eligibility": {
                        "thumbnail_policy": "ELIGIBLE_ONLY",
                        "thumbnail_eligibility": {
                          "candidate_selectors": [
                            {
                              "selector": "figure img",
                              "url_attribute": "src",
                              "credit_container_selector": "figure",
                              "credit_selectors": []
                            }
                          ],
                          "allowed_credit_fragments": ["VOA"],
                          "denied_credit_fragments": []
                        }
                      }
                    }
                    """,
                    SourcePolicy.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("creditSelectors must not be empty");
  }
}
