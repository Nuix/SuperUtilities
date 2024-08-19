package com.nuix.superutilities.misc;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class PrimitiveTypeParser {
    public static final Function<String, Object> jodaTimeAutomaticParsing = new Function<>() {
        private final List<Pair<Pattern, DateTimeFormatter>> formats = List.of(
                // 2017-12-11T15:51:24Z
                // 2022-04-20T17:12:37Z
                Pair.of(
                        Pattern.compile("^\\d{4}-[01][0-9]-[0-3][0-9][tT][0-2][0-9]:[0-5][0-9]:\\d{2}Z$"),
                        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(DateTimeZone.UTC)
                ),

                // 2017-12-11T15:51:24.000-07:00
                Pair.of(
                        Pattern.compile("^\\d{4}-[01][0-9]-[0-3][0-9][tT][0-2][0-9]:[0-5][0-9]:\\d{2}\\.\\d{3}[+\\-]\\d{2}:\\d{2}$"),
                        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
                ),

                // 6/26/2024  1:50:33 PM
                Pair.of(
                        Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}\\s{2}\\d{1,2}:\\d{2}:\\d{2} [AP]M"),
                        DateTimeFormat.forPattern("M/d/yyyy  h:m:s a").withZone(DateTimeZone.UTC)
                ),

                // 26  Jun 2024 13:50:33
                Pair.of(
                        Pattern.compile("\\d{1,2}\\s{2}[A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}:\\d{2}"),
                        DateTimeFormat.forPattern("dd  MMM yyyy HH:mm:ss").withZone(DateTimeZone.UTC)
                )
        );

        @Override
        public Object apply(String s) {
            for (Pair<Pattern, DateTimeFormatter> format : formats) {
                Matcher matcher = format.getLeft().matcher(s);
                if (matcher.matches()) {
                    return format.getRight().parseDateTime(s);
                }
            }
            return null;
        }
    };

    public static final Function<String, Object> durationAutomaticParsing = new Function<>() {
        private final List<Pattern> formatValidators = List.of(
                Pattern.compile("(?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<seconds>[0-9]{2})")
        );

        @Override
        public Object apply(String s) {
            for (Pattern pattern : formatValidators) {
                Matcher m = pattern.matcher(s);
                if (m.matches()) {
                    long hours = Long.parseLong(m.group("hours"));
                    long minutes = Long.parseLong(m.group("minutes"));
                    long seconds = Long.parseLong(m.group("seconds"));
                    long millis = ((hours * 3600) + (minutes * 60) + seconds) * 1000;
                    return new Duration(millis);
                }
            }
            return null;
        }
    };

    public static final Function<String, Object> numericParser = new Function<>() {
        private final Pattern formatValidator = Pattern.compile("^[+\\-]?[0-9]+$");

        @Override
        public Object apply(String s) {
            if (!formatValidator.matcher(s.trim()).matches()) {
                return null;
            } else {
                return Long.parseLong(s.trim());
            }
        }
    };

    public static final Function<String, Object> decimalParser = new Function<>() {
        private final Pattern formatValidator = Pattern.compile("^[+\\-]?[0-9]+\\.[0-9]*$");

        @Override
        public Object apply(String s) {
            if (!formatValidator.matcher(s.trim()).matches()) {
                return null;
            } else {
                return Double.parseDouble(s.trim());
            }
        }
    };

    public static final Function<String, Object> booleanParser = new Function<>() {
        private final Pattern formatValidator = Pattern.compile("^(true)|(false)$");

        @Override
        public Object apply(String s) {
            if (!formatValidator.matcher(s.trim().toLowerCase()).matches()) {
                return null;
            } else {
                return Boolean.parseBoolean(s.trim());
            }
        }
    };

    public static final Function<String, Object> yesNoBooleanParser = new Function<>() {
        private final Pattern formatValidator = Pattern.compile("^(yes)|(no)$", Pattern.CASE_INSENSITIVE);

        @Override
        public Object apply(String s) {
            if (!formatValidator.matcher(s.trim().toLowerCase()).matches()) {
                return null;
            } else {
                return Boolean.parseBoolean(s.trim());
            }
        }
    };

    @Getter(lazy = true)
    private static final PrimitiveTypeParser standard = buildImmutableStandard();

    private static PrimitiveTypeParser buildImmutableStandard() {
        PrimitiveTypeParser result = buildStandardCopy();
        result.typeParsers = ImmutableList.copyOf(result.typeParsers);
        return result;
    }

    public static PrimitiveTypeParser buildStandardCopy() {
        PrimitiveTypeParser result = new PrimitiveTypeParser();
        result.getTypeParsers().add(jodaTimeAutomaticParsing);
        result.getTypeParsers().add(durationAutomaticParsing);
        result.getTypeParsers().add(numericParser);
        result.getTypeParsers().add(decimalParser);
        result.getTypeParsers().add(booleanParser);
        result.getTypeParsers().add(yesNoBooleanParser);
        return result;
    }

    private List<Function<String, Object>> typeParsers;

    public PrimitiveTypeParser() {
        typeParsers = new ArrayList<>();
    }

    public Object parseWithFallback(String input, Object fallback) {
        if (input == null || input.trim().isBlank()) {
            return fallback;
        } else {
            Object parsedValue = null;
            for (Function<String, Object> parser : typeParsers) {
                parsedValue = parser.apply(input);
                if (parsedValue != null) {
                    break;
                }
            }

            if (parsedValue == null) {
                return fallback;
            } else {
                return parsedValue;
            }
        }
    }

    public Object parse(String input) {
        return parseWithFallback(input, input);
    }

    public void enrichInPlace(Map<String, Object> input) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                String stringValue = (String) value;
                Object parsedValue = parse(stringValue);
                input.put(key, parsedValue);
            }
        }
    }

    public Map<String, Object> parseAndCopy(Map<String, ?> input) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                String stringValue = (String) value;
                Object parsedValue = parse(stringValue);
                result.put(key, parsedValue);
            }
        }
        return result;
    }

    public Map<String, Object> parseAndCopy(Map<String, ?> input, Function<String, String> keyMapper) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                String stringValue = (String) value;
                Object parsedValue = parse(stringValue);
                result.put(keyMapper.apply(key), parsedValue);
            }
        }
        return result;
    }
}
