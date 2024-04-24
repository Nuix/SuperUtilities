package com.nuix.superutilities.misc;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

@Getter
public class PrimitiveTypeParser {
    public static final Function<String, Object> jodaTimeAutomaticParsing = new Function<>() {
        private final List<Pattern> formatValidators = List.of(
                // yyyy-MM-dd'T'HH:mm:ss'Z'
                // 2017-12-11T15:51:24Z
                // 2022-04-20T17:12:37Z
                Pattern.compile("^\\d{4}-[01][0-9]-[0-3][0-9][tT][0-2][0-9]:[0-5][0-9]:\\d{2}Z$"),

                // yyyy-MM-dd'T'HH:mm:ss.SSSZZ
                // 2017-12-11T15:51:24.000-07:00
                Pattern.compile("^\\d{4}-[01][0-9]-[0-3][0-9][tT][0-2][0-9]:[0-5][0-9]:\\d{2}\\.\\d{3}[+\\-]\\d{2}:\\d{2}$")
        );

        @Override
        public Object apply(String s) {
            if (formatValidators.stream()
                    .noneMatch(p -> p.matcher(s.trim()).matches())) {
                return null;
            } else {
                return DateTime.parse(s.trim());
            }
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
        result.getTypeParsers().add(numericParser);
        result.getTypeParsers().add(decimalParser);
        result.getTypeParsers().add(booleanParser);
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

    public void enrichInPlace(Map<String,Object> input) {
        for(Map.Entry<String,Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(value instanceof String) {
                String stringValue = (String) value;
                Object parsedValue = parse(stringValue);
                input.put(key,parsedValue);
            }
        }
    }
}
