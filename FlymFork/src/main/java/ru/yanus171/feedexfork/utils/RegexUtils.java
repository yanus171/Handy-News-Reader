package ru.yanus171.feedexfork.utils;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows to create timeoutable regular expressions.
 *
 * Limitations: Can only throw RuntimeException. Decreases performance.
 *
 * Posted by Kris in stackoverflow.
 *
 * Modified by dgoiko to  ejecute timeout check only every n chars.
 * Now timeout < 0 means no timeout.
 *
 * @author Kris https://stackoverflow.com/a/910798/9465588
 *
 */
public class RegexUtils {

    public static long foo = 0;

    // demonstrates behavior for regular expression running into catastrophic backtracking for given input
    public static void main(String[] args) {
        long millis = System.currentTimeMillis();
        // This checkInterval produces a < 500 ms delay. Higher checkInterval will produce higher delays on timeout.
        Matcher matcher = createMatcherWithTimeout(
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "(x+x+)+y", 10000, 30000000);
        try {
            System.out.println(matcher.matches());
        } catch (RuntimeException e) {
            System.out.println("Operation timed out after " + (System.currentTimeMillis() - millis) + " milliseconds");
        }
        System.out.print(foo);
    }

    public static Matcher createMatcherWithTimeout(String stringToMatch, String regularExpression, long timeoutMillis,
                                                   int checkInterval) {
        Pattern pattern = Pattern.compile(regularExpression);
        return createMatcherWithTimeout(stringToMatch, pattern, timeoutMillis, checkInterval);
    }

    public static Matcher createMatcherWithTimeout(String stringToMatch, Pattern regularExpressionPattern,
                                                   long timeoutMillis, int checkInterval) {
        CharSequence charSequence = new TimeoutRegexCharSequence(stringToMatch, timeoutMillis, stringToMatch,
                                                                 regularExpressionPattern.pattern(), checkInterval);
        return regularExpressionPattern.matcher(charSequence);
    }

    private static class TimeoutRegexCharSequence implements CharSequence {

        private final CharSequence inner;

        private final long timeoutMillis;

        private final long timeoutTime;

        private final String stringToMatch;

        private final String regularExpression;

        private final int checkInterval;

        private int attempt;

        TimeoutRegexCharSequence(CharSequence inner, long timeoutMillis, String stringToMatch,
                                 String regularExpression, int checkInterval) {
            super();
            this.inner = inner;
            this.timeoutMillis = timeoutMillis;
            this.stringToMatch = stringToMatch;
            this.regularExpression = regularExpression;
            timeoutTime = System.currentTimeMillis() + timeoutMillis;
            this.checkInterval = checkInterval;
            this.attempt = 0;
        }

        @Override
        public char charAt(int index) {
            if (this.attempt == this.checkInterval) {
                foo++;
                if (System.currentTimeMillis() > timeoutTime) {
                    throw new RegexpTimeoutException(regularExpression, stringToMatch, timeoutMillis);
                }
                this.attempt = 0;
            } else {
                this.attempt++;
            }

            return inner.charAt(index);
        }

        @Override
        public int length() {
            return inner.length();
        }

        @NotNull
        @Override
        public CharSequence subSequence(int start, int end) {
            return new TimeoutRegexCharSequence(inner.subSequence(start, end), timeoutMillis, stringToMatch,
                                                regularExpression, checkInterval);
        }

        @NotNull
        @Override
        public String toString() {
            return inner.toString();
        }
    }

}

class RegexpTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 6437153127902393756L;

    public RegexpTimeoutException(String regularExpression, String stringToMatch, long timeoutMillis) {
        super("Timeout occurred after " + timeoutMillis + "ms while processing regular expression '"
                  + regularExpression + "' on input '" + stringToMatch + "'!");
    }

}