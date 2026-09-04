package com.localfit.domain.subway.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * SubwayStationNameNormalizer 단위 테스트
 */
public class SubwayStationNameNormalizerTest {
    private final SubwayStationNameNormalizer normalizer = new SubwayStationNameNormalizer();

    @Nested
    @DisplayName("접미사 처리")
    class Suffix {

        @ParameterizedTest
        @DisplayName("끝의 '역' 접미사를 제거한다")
        @CsvSource({
                "창동역, 창동",
                "김포공항역, 김포공항",
                "별내역, 별내"
        })
        void removesSuffix(String raw, String expected) {
            assertThat(normalizer.normalize(raw)).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("접미사가 없는 역명은 그대로 반환한다")
        @CsvSource({
                "창동, 창동",
                "잠실, 잠실"
        })
        void keepsNameWithoutSuffix(String raw, String expected) {
            assertThat(normalizer.normalize(raw)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("괄호 부기명 처리")
    class Parentheses {

        @ParameterizedTest
        @DisplayName("괄호 안 부기명을 제거한다")
        @CsvSource({
                "양재(서초구청), 양재",
                "강변(동서울터미널), 강변",
                "동대문역사문화공원(DDP), 동대문역사문화공원",
                "총신대입구(이수), 총신대입구"
        })
        void removesParentheses(String raw, String expected) {
            assertThat(normalizer.normalize(raw)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class Exceptions {

        @Test
        @DisplayName("서울역은 '역'이 공식 역명의 일부이므로 유지한다")
        void seoulStation_keepsSuffix() {
            assertThat(normalizer.normalize("서울역")).isEqualTo("서울역");
            assertThat(normalizer.normalize("서울역(경의선)")).isEqualTo("서울역");
        }

        @Test
        @DisplayName("신촌(지하)는 괄호가 역 구분자이므로 유지한다")
        void sinchonUnderground_keepsParentheses() {
            // 2호선 신촌역(지하)과 경의중앙선 신촌역은 별개 역이다
            assertThat(normalizer.normalize("신촌(지하)")).isEqualTo("신촌(지하)");
            assertThat(normalizer.normalize("신촌역")).isEqualTo("신촌");
        }
    }

    @Test
    @DisplayName("null이나 빈 문자열은 빈 문자열을 반환한다")
    void nullOrBlank_returnsEmpty() {
        assertThat(normalizer.normalize(null)).isEmpty();
        assertThat(normalizer.normalize("  ")).isEmpty();
    }
}
