/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser.token;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TimeDuration class represents a time duration value with a time unit (ns, ms, s, m, h, d).
 * It parses a string representation of a time duration and provides methods to retrieve
 * the value in different time units.
 */
public class TimeDuration implements Token {
  private static final Pattern PATTERN = Pattern.compile("([\\d.]+)\\s*([nNmMsShHdD]{1,2})");
  private final String value;
  private final double numValue;
  private final String unit;
  private final long nanos;

  /**
   * Constructor to create a TimeDuration from a string.
   *
   * @param value String representation of the time duration, e.g. "100ms", "5s", "2.5h"
   * @throws IllegalArgumentException If the input string doesn't match the expected format
   */
  public TimeDuration(String value) {
    this.value = value;
    Matcher matcher = PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        String.format("Invalid time duration format: %s. Expected format: <number><unit>, e.g. 10ms", value));
    }
    
    this.numValue = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2).toLowerCase(Locale.ENGLISH);
    this.nanos = calculateNanos(this.numValue, this.unit);
  }

  /**
   * Calculates the equivalent nanoseconds from a value and unit.
   *
   * @param value Numeric value
   * @param unit Unit string (ns, ms, s, m, h, d)
   * @return Number of nanoseconds
   */
  private long calculateNanos(double value, String unit) {
    switch (unit.toLowerCase(Locale.ENGLISH)) {
      case "ns":
        return (long) value;
      case "ms":
        return (long) (value * TimeUnit.MILLISECONDS.toNanos(1));
      case "s":
        return (long) (value * TimeUnit.SECONDS.toNanos(1));
      case "m":
        return (long) (value * TimeUnit.MINUTES.toNanos(1));
      case "h":
        return (long) (value * TimeUnit.HOURS.toNanos(1));
      case "d":
        return (long) (value * TimeUnit.DAYS.toNanos(1));
      default:
        throw new IllegalArgumentException("Unknown time unit: " + unit);
    }
  }

  /**
   * Returns the original string value of this token.
   *
   * @return the original string value
   */
  @Override
  public Object value() {
    return value;
  }

  /**
   * Returns the token type - TIME_DURATION.
   *
   * @return TokenType.TIME_DURATION
   */
  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  /**
   * Converts this object to a JSON representation.
   *
   * @return JsonElement representing this object
   */
  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", type().name());
    object.addProperty("value", value);
    object.addProperty("nanos", nanos);
    return object;
  }

  /**
   * Returns the duration in nanoseconds.
   *
   * @return The duration in nanoseconds
   */
  public long getNanos() {
    return nanos;
  }

  /**
   * Returns the duration in milliseconds.
   *
   * @return The duration in milliseconds
   */
  public double getMillis() {
    // Convert to double for precise floating-point representation
    return nanos / (double) TimeUnit.MILLISECONDS.toNanos(1);
  }

  /**
   * Returns the duration in seconds.
   *
   * @return The duration in seconds
   */
  public double getSeconds() {
    // Convert to double for precise floating-point representation
    return nanos / (double) TimeUnit.SECONDS.toNanos(1);
  }

  /**
   * Returns the duration in minutes.
   *
   * @return The duration in minutes
   */
  public double getMinutes() {
    // Convert to double for precise floating-point representation
    return nanos / (double) TimeUnit.MINUTES.toNanos(1);
  }

  /**
   * Returns the duration in hours.
   *
   * @return The duration in hours
   */
  public double getHours() {
    // Convert to double for precise floating-point representation
    return nanos / (double) TimeUnit.HOURS.toNanos(1);
  }

  /**
   * Returns the duration in days.
   *
   * @return The duration in days
   */
  public double getDays() {
    // Convert to double for precise floating-point representation
    return nanos / (double) TimeUnit.DAYS.toNanos(1);
  }

  /**
   * Returns the numeric value parsed from the original string.
   *
   * @return The numeric value
   */
  public double getNumericValue() {
    return numValue;
  }

  /**
   * Returns the unit parsed from the original string.
   *
   * @return The unit string (ns, ms, s, m, h, d)
   */
  public String getUnit() {
    return unit;
  }

  @Override
  public String toString() {
    return value;
  }
}
