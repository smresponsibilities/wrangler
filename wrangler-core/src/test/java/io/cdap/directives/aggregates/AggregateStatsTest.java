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

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link AggregateStats} directive.
 */
public class AggregateStatsTest {
  // Constants for byte conversions
  private static final long BYTES_PER_KB = 1024L;
  private static final long BYTES_PER_MB = 1024L * 1024L;
  private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;
  private static final long BYTES_PER_TB = 1024L * 1024L * 1024L * 1024L;
  private static final long BYTES_PER_PB = 1024L * 1024L * 1024L * 1024L * 1024L;
  
  // Constants for time conversions (in nanoseconds)
  private static final long NANOS_PER_MS = 1_000_000L;
  private static final long NANOS_PER_SECOND = 1_000_000_000L;
  private static final long NANOS_PER_MINUTE = 60L * NANOS_PER_SECOND;
  private static final long NANOS_PER_HOUR = 60L * NANOS_PER_MINUTE;
  private static final long NANOS_PER_DAY = 24L * NANOS_PER_HOUR;
  
  // Constants for assertions
  private static final double DELTA = 0.1;

  /**
   * Tests basic byte size aggregation with various units.
   */
  @Test
  public void testBasicByteSizeAggregation() throws Exception {
    // Define directive for byte size aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :file_size :size_statistics 'byte'"
    };
    
    // Create test rows with different byte size values
    List<Row> inputRows = Arrays.asList(
      new Row("file_size", "10B"),
      new Row("file_size", "20KB"),
      new Row("file_size", "1.5MB"),
      new Row("file_size", "5KB"),
      new Row("file_size", "10KB")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned for aggregate directives
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics from the result
    Map<String, Object> sizeStatistics = (Map<String, Object>) processedRows.get(0).getValue("size_statistics");
    
    // Verify count is correct
    Assert.assertEquals(5L, sizeStatistics.get("count"));
    
    // Calculate expected sum: 10B + 20KB + 1.5MB + 5KB + 10KB = 1572874 bytes
    long expectedTotalBytes = (long) (10 + 20 * 1024 + 1.5 * 1024 * 1024 + 5 * 1024 + 10 * 1024);
    Assert.assertEquals(expectedTotalBytes, Double.valueOf(sizeStatistics.get("sum").toString()).longValue());
    
    // Verify min (10 bytes), max (1.5MB in bytes), and unit counts
    Assert.assertEquals(10.0, Double.valueOf(sizeStatistics.get("min").toString()), 0.1);
    Assert.assertEquals(1572864.0, Double.valueOf(sizeStatistics.get("max").toString()), 0.1);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) sizeStatistics.get("units");
    Assert.assertEquals(1L, unitDistribution.get("B"));
    Assert.assertEquals(3L, unitDistribution.get("KB"));
    Assert.assertEquals(1L, unitDistribution.get("MB"));
    
    // Verify converted summary values exist
    Assert.assertTrue(sizeStatistics.containsKey("sum_kb"));
    Assert.assertTrue(sizeStatistics.containsKey("sum_mb"));
    Assert.assertTrue(sizeStatistics.containsKey("sum_gb"));
  }
  
  /**
   * Tests basic time duration aggregation with various units.
   */
  @Test
  public void testBasicTimeDurationAggregation() throws Exception {
    // Define directive for time duration aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :response_time :timing_statistics 'time'"
    };
    
    // Create test rows with different time duration values
    List<Row> inputRows = Arrays.asList(
      new Row("response_time", "100ms"),
      new Row("response_time", "2.5s"),
      new Row("response_time", "10s"),
      new Row("response_time", "1m"),
      new Row("response_time", "50ms")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned for aggregate directives
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics from the result
    Map<String, Object> timingStatistics = (Map<String, Object>) processedRows.get(0).getValue("timing_statistics");
    
    // Verify count is correct
    Assert.assertEquals(5L, timingStatistics.get("count"));
    
    // Calculate expected sum in nanoseconds:
    // 100ms + 2.5s + 10s + 1m + 50ms
    // = 100_000_000 + 2_500_000_000 + 10_000_000_000 + 60_000_000_000 + 50_000_000
    // = 72,650,000,000 nanoseconds
    long expectedTotalNanos = 100_000_000L + 2_500_000_000L + 10_000_000_000L + 60_000_000_000L + 50_000_000L;
    Assert.assertEquals(expectedTotalNanos, Double.valueOf(timingStatistics.get("sum_nanos").toString()).longValue());
    
    // Verify min (50ms in nanos), max (1m in nanos)
    Assert.assertEquals(50_000_000.0, Double.valueOf(timingStatistics.get("min_nanos").toString()), 0.1);
    Assert.assertEquals(60_000_000_000.0, Double.valueOf(timingStatistics.get("max_nanos").toString()), 0.1);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) timingStatistics.get("units");
    Assert.assertEquals(2L, unitDistribution.get("ms"));
    Assert.assertEquals(2L, unitDistribution.get("s"));
    Assert.assertEquals(1L, unitDistribution.get("m"));
    
    // Verify converted summary values exist
    Assert.assertTrue(timingStatistics.containsKey("sum_ms"));
    Assert.assertTrue(timingStatistics.containsKey("sum_s"));
    Assert.assertTrue(timingStatistics.containsKey("sum_m"));
    Assert.assertTrue(timingStatistics.containsKey("sum_h"));
  }
  
  /**
   * Tests the directive's handling of invalid and missing values.
   */
  @Test
  public void testMixedAndInvalidValues() throws Exception {
    // Test byte size with mixed valid/invalid values
    String[] byteSizeDirectives = new String[] {
      "aggregate-stats :file_size :size_stats 'byte'"
    };
    
    List<Row> byteSizeRows = Arrays.asList(
      new Row("file_size", "10KB"),
      new Row("file_size", "invalid"), // Invalid value
      new Row("file_size", "5MB"),
      new Row("file_size", null), // Null value
      new Row("file_size", "20KB")
    );
    
    List<Row> byteSizeResults = TestingRig.execute(byteSizeDirectives, byteSizeRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, byteSizeResults.size());
    
    Map<String, Object> sizeStats = (Map<String, Object>) byteSizeResults.get(0).getValue("size_stats");
    
    // Should only count valid values (10KB, 5MB, 20KB = 3 items)
    Assert.assertEquals(3L, sizeStats.get("count"));
    
    // Calculate expected sum: 10KB + 5MB + 20KB = 5,242,880 bytes
    long expectedTotalBytes = 10 * 1024 + 5 * 1024 * 1024 + 20 * 1024;
    Assert.assertEquals(expectedTotalBytes, Double.valueOf(sizeStats.get("sum").toString()).longValue());
    
    // Verify unit distribution
    Map<String, Object> sizeUnitDistribution = (Map<String, Object>) sizeStats.get("units");
    Assert.assertEquals(2L, sizeUnitDistribution.get("KB"));
    Assert.assertEquals(1L, sizeUnitDistribution.get("MB"));
    
    // Test time duration with mixed valid/invalid values
    String[] timeDirectives = new String[] {
      "aggregate-stats :process_time :time_stats 'time'"
    };
    
    List<Row> timeRows = Arrays.asList(
      new Row("process_time", "100ms"),
      new Row("process_time", "invalid"), // Invalid value
      new Row("process_time", "5s"),
      new Row("process_time", null), // Null value
      new Row("process_time", "10m")
    );
    
    List<Row> timeResults = TestingRig.execute(timeDirectives, timeRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, timeResults.size());
    
    Map<String, Object> timeStats = (Map<String, Object>) timeResults.get(0).getValue("time_stats");
    
    // Should only count valid values (100ms, 5s, 10m = 3 items)
    Assert.assertEquals(3L, timeStats.get("count"));
    
    // Calculate expected sum in nanoseconds: 100ms + 5s + 10m
    long expectedNanos = 100_000_000L + 5_000_000_000L + 600_000_000_000L;
    Assert.assertEquals(expectedNanos, Double.valueOf(timeStats.get("sum_nanos").toString()).longValue());
    
    // Verify unit distribution
    Map<String, Object> timeUnitDistribution = (Map<String, Object>) timeStats.get("units");
    Assert.assertEquals(1L, timeUnitDistribution.get("ms"));
    Assert.assertEquals(1L, timeUnitDistribution.get("s"));
    Assert.assertEquals(1L, timeUnitDistribution.get("m"));
  }
  
  /**
   * Tests byte size aggregation with zero values.
   */
  @Test
  public void testByteSizeAggregationWithZeroValues() throws Exception {
    // Define directive for byte size aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :disk_usage :storage_stats 'byte'"
    };
    
    // Create test rows with zero values
    List<Row> inputRows = Arrays.asList(
      new Row("disk_usage", "0B"),
      new Row("disk_usage", "0KB"), 
      new Row("disk_usage", "1024KB")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> storageStats = (Map<String, Object>) processedRows.get(0).getValue("storage_stats");
    
    // Verify results
    Assert.assertEquals(3L, storageStats.get("count"));
    Assert.assertEquals(0.0, Double.valueOf(storageStats.get("min").toString()), 0.1);
    Assert.assertEquals(1048576.0, Double.valueOf(storageStats.get("max").toString()), 0.1);
    
    // Verify average calculation
    double expectedAverage = (0 + 0 + 1048576) / 3.0;
    Assert.assertEquals(expectedAverage, Double.valueOf(storageStats.get("avg").toString()), 0.01);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) storageStats.get("units");
    Assert.assertEquals(1L, unitDistribution.get("B"));
    Assert.assertEquals(2L, unitDistribution.get("KB"));
  }
  
  /**
   * Tests time duration aggregation with zero values.
   */
  @Test
  public void testTimeDurationAggregationWithZeroValues() throws Exception {
    // Define directive for time duration aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :execution_time :perf_stats 'time'"
    };
    
    // Create test rows with zero values
    List<Row> inputRows = Arrays.asList(
      new Row("execution_time", "0ms"),
      new Row("execution_time", "0s"), 
      new Row("execution_time", "500ms")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> performanceStats = (Map<String, Object>) processedRows.get(0).getValue("perf_stats");
    
    // Verify results
    Assert.assertEquals(3L, performanceStats.get("count"));
    Assert.assertEquals(0.0, Double.valueOf(performanceStats.get("min_nanos").toString()), 0.1);
    Assert.assertEquals(500_000_000.0, Double.valueOf(performanceStats.get("max_nanos").toString()), 0.1);
    
    // Verify average calculation
    double expectedAverage = (0 + 0 + 500_000_000) / 3.0;
    Assert.assertEquals(expectedAverage, Double.valueOf(performanceStats.get("avg_nanos").toString()), 0.01);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) performanceStats.get("units");
    Assert.assertEquals(1L, unitDistribution.get("ms"));
    Assert.assertEquals(2L, unitDistribution.get("s"));
  }
  
  /**
   * Tests byte size aggregation with large values.
   */
  @Test
  public void testByteSizeAggregationWithLargeValues() throws Exception {
    // Define directive for byte size aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :large_files :large_file_stats 'byte'"
    };
    
    // Create test rows with large byte size values
    List<Row> inputRows = Arrays.asList(
      new Row("large_files", "1GB"),
      new Row("large_files", "2.5GB"),
      new Row("large_files", "0.5TB"),
      new Row("large_files", "0.01PB")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> largeFileStats = (Map<String, Object>) processedRows.get(0).getValue("large_file_stats");
    
    // Verify count
    Assert.assertEquals(4L, largeFileStats.get("count"));
    
    // Verify min value (1GB in bytes)
    Assert.assertEquals(1073741824.0, Double.valueOf(largeFileStats.get("min").toString()), 1.0);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) largeFileStats.get("units");
    Assert.assertEquals(2L, unitDistribution.get("GB"));
    Assert.assertEquals(1L, unitDistribution.get("TB"));
    Assert.assertEquals(1L, unitDistribution.get("PB"));
    
    // Verify converted values are present
    Assert.assertTrue(largeFileStats.containsKey("sum_kb"));
    Assert.assertTrue(largeFileStats.containsKey("sum_mb"));
    Assert.assertTrue(largeFileStats.containsKey("sum_gb"));
  }
  
  /**
   * Tests time duration aggregation with large values.
   */
  @Test
  public void testTimeDurationAggregationWithLargeValues() throws Exception {
    // Define directive for time duration aggregation
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :process_duration :long_running_stats 'time'"
    };
    
    // Create test rows with large time duration values
    List<Row> inputRows = Arrays.asList(
      new Row("process_duration", "1h"),
      new Row("process_duration", "2.5h"),
      new Row("process_duration", "0.5d"),
      new Row("process_duration", "30h")
    );
    
    // Execute the directive on the input rows
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, inputRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> longRunningStats = (Map<String, Object>) processedRows.get(0).getValue("long_running_stats");
    
    // Verify count
    Assert.assertEquals(4L, longRunningStats.get("count"));
    
    // Verify min value (1h in nanoseconds)
    Assert.assertEquals(3600.0 * 1_000_000_000, Double.valueOf(longRunningStats.get("min_nanos").toString()), 1.0);
    
    // Verify unit distribution
    Map<String, Object> unitDistribution = (Map<String, Object>) longRunningStats.get("units");
    Assert.assertEquals(3L, unitDistribution.get("h"));
    Assert.assertEquals(1L, unitDistribution.get("d"));
    
    // Verify converted values are present
    Assert.assertTrue(longRunningStats.containsKey("sum_ms"));
    Assert.assertTrue(longRunningStats.containsKey("sum_s"));
    Assert.assertTrue(longRunningStats.containsKey("sum_m"));
    Assert.assertTrue(longRunningStats.containsKey("sum_h"));
  }
  
  /**
   * Tests directive behavior with empty input rows.
   */
  @Test
  public void testEmptyInputRows() throws Exception {
    // Define directive
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :size :stats 'byte'"
    };
    
    // Create empty list of rows
    List<Row> emptyInputRows = Collections.emptyList();
    
    // Execute the directive on the empty input
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, emptyInputRows);
    
    // Should return empty result
    Assert.assertTrue(processedRows.isEmpty());
  }
  
  /**
   * Tests directive behavior with all null values.
   */
  @Test
  public void testAllNullValues() throws Exception {
    // Define directive
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :size :stats 'byte'"
    };
    
    // Create rows with all null values
    List<Row> nullValueRows = Arrays.asList(
      new Row("size", null),
      new Row("size", null),
      new Row("othercolumn", "value") // Missing the size column
    );
    
    // Execute the directive on the input with null values
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, nullValueRows);
    
    // Verify that exactly one row is returned (even if count is 0)
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> sizeStats = (Map<String, Object>) processedRows.get(0).getValue("stats");
    
    // Should have count of 0 since all values were null or missing
    Assert.assertEquals(0L, sizeStats.get("count"));
    
    // Sum should be 0
    Assert.assertEquals(0.0, Double.valueOf(sizeStats.get("sum").toString()), 0.001);
  }
  
  /**
   * Tests byte size statistics with all the same value.
   */
  @Test
  public void testByteStatsOnAllSameValues() throws Exception {
    // Define directive
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :uniform_size :uniform_stats 'byte'"
    };
    
    // Create rows with all the same value
    List<Row> uniformRows = Arrays.asList(
      new Row("uniform_size", "1KB"),
      new Row("uniform_size", "1KB"),
      new Row("uniform_size", "1KB"),
      new Row("uniform_size", "1KB")
    );
    
    // Execute the directive on the uniform input
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, uniformRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> uniformStats = (Map<String, Object>) processedRows.get(0).getValue("uniform_stats");
    
    // Verify min = max = avg = 1KB in bytes
    Assert.assertEquals(1024.0, Double.valueOf(uniformStats.get("min").toString()), 0.001);
    Assert.assertEquals(1024.0, Double.valueOf(uniformStats.get("max").toString()), 0.001);
    Assert.assertEquals(1024.0, Double.valueOf(uniformStats.get("avg").toString()), 0.001);
    
    // Unit distribution should only have KB with count 4
    Map<String, Object> unitDistribution = (Map<String, Object>) uniformStats.get("units");
    Assert.assertEquals(4L, unitDistribution.get("KB"));
    Assert.assertEquals(1, unitDistribution.size()); // Only KB should be present
  }
  
  /**
   * Tests time statistics with all the same value.
   */
  @Test
  public void testTimeStatsOnAllSameValues() throws Exception {
    // Define directive
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :uniform_time :uniform_stats 'time'"
    };
    
    // Create rows with all the same value
    List<Row> uniformRows = Arrays.asList(
      new Row("uniform_time", "500ms"),
      new Row("uniform_time", "500ms"),
      new Row("uniform_time", "500ms"),
      new Row("uniform_time", "500ms")
    );
    
    // Execute the directive on the uniform input
    List<Row> processedRows = TestingRig.execute(aggregateDirectives, uniformRows);
    
    // Verify that exactly one row is returned
    Assert.assertEquals(1, processedRows.size());
    
    // Extract the aggregated statistics
    Map<String, Object> uniformStats = (Map<String, Object>) processedRows.get(0).getValue("uniform_stats");
    
    // Verify min = max = avg = 500ms in nanoseconds
    long valueInNanos = 500_000_000L;
    Assert.assertEquals(valueInNanos, Double.valueOf(uniformStats.get("min_nanos").toString()).longValue());
    Assert.assertEquals(valueInNanos, Double.valueOf(uniformStats.get("max_nanos").toString()).longValue());
    Assert.assertEquals(valueInNanos, Double.valueOf(uniformStats.get("avg_nanos").toString()).longValue());
    
    // Unit distribution should only have ms with count 4
    Map<String, Object> unitDistribution = (Map<String, Object>) uniformStats.get("units");
    Assert.assertEquals(4L, unitDistribution.get("ms"));
    Assert.assertEquals(1, unitDistribution.size()); // Only ms should be present
  }
  
  /**
   * Tests that invalid type parameter throws appropriate exception.
   */
  @Test(expected = RecipeException.class)
  public void testInvalidTypeParameter() throws Exception {
    // Define directive with invalid type
    String[] aggregateDirectives = new String[] {
      "aggregate-stats :size :stats 'invalid'" // Neither 'byte' nor 'time'
    };
    
    List<Row> inputRows = Arrays.asList(
      new Row("size", "10KB")
    );
    
    // Should throw RecipeException due to invalid type parameter
    TestingRig.execute(aggregateDirectives, inputRows);
  }
}
