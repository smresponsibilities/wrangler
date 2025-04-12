/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.directives.transformation;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link FindAndReplace} directive which performs pattern-based string replacements.
 * This class validates various use cases for find-and-replace functionality
 * including basic substitution, regex patterns, and special character handling.
 */
public class FindAndReplaceTest {
  
  /**
   * Tests the basic sed-like global replace functionality.
   * Removes all double quotes from text in the 'body' column.
   */
  @Test
  public void testBasicGlobalReplace() throws Exception {
    String[] directives = new String[] {
      "find-and-replace body s/\"//g"
    };

    List<Row> rows = Arrays.asList(
      new Row("body", "07/29/2013,Debt collection,\"Other (i.e. phone, health club, etc.)\",Cont'd attempts collect " +
        "debt not owed,Debt is not mine,,,\"NRA Group, LLC\",VA,20147,,N/A,Web,08/07/2013,Closed with non-monetary " +
        "relief,Yes,No,467801"),
      new Row("body", "07/29/2013,Mortgage,Conventional fixed mortgage,\"Loan servicing, payments, escrow account\",," +
        ",,Franklin Credit Management,CT,06106,,N/A,Web,07/30/2013,Closed with explanation,Yes,No,475823")
    );

    rows = TestingRig.execute(directives, rows);

    Assert.assertEquals(2, rows.size());
    Assert.assertEquals("07/29/2013,Debt collection,Other (i.e. phone, health club, etc.),Cont'd " +
                          "attempts collect debt not owed,Debt is not mine,,,NRA Group, LLC,VA,20147,,N/A," +
                          "Web,08/07/2013,Closed with non-monetary relief,Yes,No,467801",
                        rows.get(0).getValue("body"));
  }
  
  /**
   * Tests replacing specific pattern with another string.
   * Replaces "Yes" with "TRUE" and "No" with "FALSE" in the 'status' column.
   */
  @Test
  public void testSpecificReplacements() throws Exception {
    String[] directives = new String[] {
      "find-and-replace status s/Yes/TRUE/g",
      "find-and-replace status s/No/FALSE/g"
    };

    List<Row> rows = Arrays.asList(
      new Row("status", "Yes"),
      new Row("status", "No"),
      new Row("status", "Maybe")
    );

    rows = TestingRig.execute(directives, rows);

    Assert.assertEquals(3, rows.size());
    Assert.assertEquals("TRUE", rows.get(0).getValue("status"));
    Assert.assertEquals("FALSE", rows.get(1).getValue("status"));
    Assert.assertEquals("Maybe", rows.get(2).getValue("status"));
  }
  
  /**
   * Tests non-global replacement that only affects the first occurrence.
   * Replaces only the first occurrence of a pattern in the 'text' column.
   */
  @Test
  public void testFirstOccurrenceReplacement() throws Exception {
    String[] directives = new String[] {
      "find-and-replace text s/test/TEST/"
    };

    List<Row> rows = Arrays.asList(
      new Row("text", "This is a test with another test"),
      new Row("text", "No matches here")
    );

    rows = TestingRig.execute(directives, rows);

    Assert.assertEquals(2, rows.size());
    Assert.assertEquals("This is a TEST with another test", rows.get(0).getValue("text"));
    Assert.assertEquals("No matches here", rows.get(1).getValue("text"));
  }
  
  /**
   * Tests case-insensitive replacements.
   * Replaces patterns regardless of case using the 'i' flag.
   */
  @Test
  public void testCaseInsensitiveReplacement() throws Exception {
    String[] directives = new String[] {
      "find-and-replace text s/data/INFORMATION/gi"
    };

    List<Row> rows = Arrays.asList(
      new Row("text", "Data processing and data transformation"),
      new Row("text", "DATA ANALYSIS")
    );

    rows = TestingRig.execute(directives, rows);

    Assert.assertEquals(2, rows.size());
    Assert.assertEquals("INFORMATION processing and INFORMATION transformation", rows.get(0).getValue("text"));
    Assert.assertEquals("INFORMATION ANALYSIS", rows.get(1).getValue("text"));
  }
  
  /**
   * Tests replacement with special regex pattern using capture groups.
   * Uses regex capturing groups to modify date formats in the 'date' column.
   */
  @Test
  public void testRegexGroupReplacement() throws Exception {
    String[] directives = new String[] {
      "find-and-replace date s/(\\d{2})\\/(\\d{2})\\/(\\d{4})/$3-$1-$2/g"
    };

    List<Row> rows = Arrays.asList(
      new Row("date", "07/29/2013"),
      new Row("date", "12/15/2022")
    );

    rows = TestingRig.execute(directives, rows);

    Assert.assertEquals(2, rows.size());
    Assert.assertEquals("2013-07-29", rows.get(0).getValue("date"));
    Assert.assertEquals("2022-12-15", rows.get(1).getValue("date"));
  }
}
