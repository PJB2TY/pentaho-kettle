/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.excelinput;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.junit.ClassRule;
import org.junit.Test;
import org.pentaho.di.core.Const;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ExcelInputContentParsingTest extends BaseExcelParsingTest {
  public static final String XLSX_FILE_WITH_SHARED_STRINGS = "file_with_shared_strings.xlsx";
  public static final String XLSX_FILE_WITH_INLINED_STRINGS = "file_with_inlined_strings.xlsx";
  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();

  private static final String[] CNST_3_SHEET_NAME_ARRAY = { "Sheet1", "Sheet2", "Sheet3" };
  private static final String[] CNST_1_SHEET_NAME_ARRAY = { "Sheet1" };
  private static final int[] CNST_3_SHEET_START_ROW_ARRAY = { 23, 3, 7 };
  private static final int[] CNST_3_ZERO_INT_ARRAY = { 0, 0, 0 };
  private static final int[] CNST_1_ZERO_INT_ARRAY = { 0 };
  private static final int PDI_17765_ROW_LIMIT_SINGLE_SHEET = 10;
  private static final int PDI_17765_ROW_LIMIT_MULTIPLE_SHEET = 20;

  @Override
  public void before() {
    super.before();

    System.clearProperty( Const.KETTLE_ZIP_MAX_ENTRY_SIZE );
    System.clearProperty( Const.KETTLE_ZIP_MAX_TEXT_SIZE );
    System.clearProperty( Const.KETTLE_ZIP_MIN_INFLATE_RATIO );
  }

  @Test
  public void testXLS() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.JXL );
    init( "sample.xls" );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ) );

    process();

    check( new Object[][] { { "test", null }, { "test", "test" } } );
  }

  @Test
  public void testXLSX() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.POI );
    init( "sample.xlsx" );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ) );

    process();

    check( new Object[][] { { "test", null }, { "test", "test" } } );
  }

  @Test
  public void testXLSXStream() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( "sample.xlsx" );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ) );

    process();

    check( new Object[][] { { "test", null }, { "test", "test" } } );
  }

  @Test
  public void testODS24() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.ODS );
    init( "sample-2.4.ods" );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ) );

    process();

    check( new Object[][] { { "test", null }, { "test", "test" } } );
  }

  @Test
  public void testODS341() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.ODS );
    init( "sample-3.4.1.ods" );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ) );

    process();

    check( new Object[][] { { "AAABBC", "Nissan" }, { "AAABBC", "Nissan" }, { "AAABBC", "Nissan" }, { "AAABBC",
        "Nissan" } } );
  }

  @Test
  public void testZipBombConfiguration_Default() throws Exception {

    // First set some random values
    Long bogusMaxEntrySize = 1000L;
    ZipSecureFile.setMaxEntrySize( bogusMaxEntrySize );
    Long bogusMaxTextSize = 1000L;
    ZipSecureFile.setMaxTextSize( bogusMaxTextSize );
    Double bogusMinInflateRatio = 0.5d;
    ZipSecureFile.setMinInflateRatio( bogusMinInflateRatio );

    // Verify that the bogus values were set
    assertEquals( bogusMaxEntrySize, (Long) ZipSecureFile.getMaxEntrySize() );
    assertEquals( bogusMaxTextSize, (Long) ZipSecureFile.getMaxTextSize() );
    assertEquals( bogusMinInflateRatio, (Double) ZipSecureFile.getMinInflateRatio() );

    // Initializing the ExcelInput step should make the new values to be set
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    System.setProperty( Const.KETTLE_XLSX_ZIP_BOMB_CHECK, Boolean.TRUE.toString() );
    init( "Balance_Type_Codes.xlsx" );

    // Verify that the default values were used
    assertEquals( Const.KETTLE_ZIP_MAX_ENTRY_SIZE_DEFAULT, (Long) ZipSecureFile.getMaxEntrySize() );
    assertEquals( Const.KETTLE_ZIP_MAX_TEXT_SIZE_DEFAULT, (Long) ZipSecureFile.getMaxTextSize() );
    assertEquals( Const.KETTLE_ZIP_MIN_INFLATE_RATIO_DEFAULT, (Double) ZipSecureFile.getMinInflateRatio() );
  }

  @Test
  public void testZipBombConfiguration_CheckDisabled() throws Exception {

    Double bogusMinInflateRatio = 0.5d;
    ZipSecureFile.setMinInflateRatio( bogusMinInflateRatio );

    // Verify the Min Inflate Ratio was set
    assertEquals( bogusMinInflateRatio, (Double) ZipSecureFile.getMinInflateRatio() );

    // Initializing the ExcelInput step should make the new values to be set
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    // Disabling the zip bomb checking property
    System.setProperty( Const.KETTLE_XLSX_ZIP_BOMB_CHECK, Boolean.FALSE.toString() );
    init( "Balance_Type_Codes.xlsx" );

    assertEquals( Const.KETTLE_ZIP_NEGATIVE_MIN_INFLATE, (Double) ZipSecureFile.getMinInflateRatio() );
  }

  @Test
  public void testZipBombConfiguration() throws Exception {
    Long maxEntrySizeVal = 3L * 1024 * 1024 * 1024;
    Long maxTextSizeVal = 2L * 1024 * 1024 * 1024;
    Double minInflateRatioVal = 0.123d;

    // First set the property values
    System.setProperty( Const.KETTLE_ZIP_MAX_ENTRY_SIZE, maxEntrySizeVal.toString() );
    System.setProperty( Const.KETTLE_ZIP_MAX_TEXT_SIZE, maxTextSizeVal.toString() );
    System.setProperty( Const.KETTLE_ZIP_MIN_INFLATE_RATIO, minInflateRatioVal.toString() );
    System.setProperty( Const.KETTLE_XLSX_ZIP_BOMB_CHECK, Boolean.TRUE.toString() );
    //ExcelInput excelInput = new ExcelInput( null, null, 0, null, null );

    // Initializing the ExcelInput step should make the new values to be set

    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( "Balance_Type_Codes.xlsx" );

    // Verify that the setted values were used
    assertEquals( maxEntrySizeVal, (Long) ZipSecureFile.getMaxEntrySize() );
    assertEquals( maxTextSizeVal, (Long) ZipSecureFile.getMaxTextSize() );
    assertEquals( minInflateRatioVal, (Double) ZipSecureFile.getMinInflateRatio() );
  }

  @Test
  public void testXLSXCompressionRatioIsBig() throws Exception {

    // For this zip to be correctly handed, we need to allow a lower inflate ratio
    Double minInflateRatio = 0.007d;
    System.setProperty( Const.KETTLE_ZIP_MIN_INFLATE_RATIO, minInflateRatio.toString() );
    System.setProperty( Const.KETTLE_XLSX_ZIP_BOMB_CHECK, Boolean.TRUE.toString() );

    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( "Balance_Type_Codes.xlsx" );

    // Verify that the minimum allowed inflate ratio is the expected
    assertEquals( minInflateRatio, (Double) ZipSecureFile.getMinInflateRatio() );

    setFields( new ExcelInputField( "FIST ID", -1, -1 ), new ExcelInputField( "SOURCE SYSTEM", -1, -1 ) );

    process();

    checkErrors();
    checkContent( new Object[][] { { "FIST0200", "ACM" } } );
  }

  protected void test_PDI_17765( int rowLimit, boolean startsWithHeader, int[] startRowArr, int[] startColumnArr,
                                 String[] sheetNameArr ) throws Exception {

    // Common stuff
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );

    setFields( new ExcelInputField( "COL", -1, -1 ) );
    meta.setRowLimit( rowLimit );

    // Set scenario parameters
    meta.setStartsWithHeader( startsWithHeader );
    meta.setStartRow( startRowArr );
    meta.setStartColumn( startColumnArr );
    meta.setSheetName( sheetNameArr );
    meta.normilizeAllocation();

    init( "pdi-17765.xlsx" );

    // Process
    process();

    // Check
    checkErrors();
    assertEquals( "Wrong row count", rowLimit, rows.size() );
  }

  protected void test_PDI_17765_SingleSheet( int rowLimit, boolean startsWithHeader, int startRow, Object firstResult,
                                             Object lastResult ) throws Exception {

    test_PDI_17765( PDI_17765_ROW_LIMIT_SINGLE_SHEET, startsWithHeader, new int[] { startRow }, CNST_1_ZERO_INT_ARRAY,
      CNST_1_SHEET_NAME_ARRAY );

    // Checks
    assertEquals( "Wrong row count", PDI_17765_ROW_LIMIT_SINGLE_SHEET, rows.size() );
    assertEquals( "Wrong first result", firstResult, rows.get( 0 )[ 0 ] );
    assertEquals( "Wrong last result", lastResult, rows.get( PDI_17765_ROW_LIMIT_SINGLE_SHEET - 1 )[ 0 ] );
  }

  @Test
  public void test_PDI_17765_SingleSheet_Header_StartRow0() throws Exception {
    String firstResult = "1.0";
    String lastResult = "10.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, true, 0, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_SingleSheet_NoHeader_StartRow0() throws Exception {
    String firstResult = "col";
    String lastResult = "9.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, false, 0, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_SingleSheet_Header_StartRow5() throws Exception {
    String firstResult = "6.0";
    String lastResult = "15.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, true, 5, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_SingleSheet_NoHeader_StartRow5() throws Exception {
    String firstResult = "5.0";
    String lastResult = "14.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, false, 5, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_SingleSheet_Header_StartRow12() throws Exception {
    String firstResult = "13.0";
    String lastResult = "22.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, true, 12, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_SingleSheet_NoHeader_StartRow12() throws Exception {
    String firstResult = "12.0";
    String lastResult = "21.0";
    test_PDI_17765_SingleSheet( PDI_17765_ROW_LIMIT_SINGLE_SHEET, false, 12, firstResult, lastResult );
  }

  @Test
  public void test_PDI_17765_MultipleSheets_Header_StartRow0() throws Exception {
    String firstResult = "1.0";
    String lastResult = "20.0";
    test_PDI_17765( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET, true, CNST_3_ZERO_INT_ARRAY, CNST_3_ZERO_INT_ARRAY,
      CNST_3_SHEET_NAME_ARRAY );

    // Checks
    assertEquals( "Wrong first result", firstResult, rows.get( 0 )[ 0 ] );
    assertEquals( "Wrong last result", lastResult, rows.get( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET - 1 )[ 0 ] );
  }

  @Test
  public void test_PDI_17765_MultipleSheets_NoHeader_StartRow0() throws Exception {
    String firstResult = "col";
    String lastResult = "19.0";
    test_PDI_17765( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET, false, CNST_3_ZERO_INT_ARRAY, CNST_3_ZERO_INT_ARRAY,
      CNST_3_SHEET_NAME_ARRAY );

    // Checks
    assertEquals( "Wrong first result", firstResult, rows.get( 0 )[ 0 ] );
    assertEquals( "Wrong last result", lastResult, rows.get( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET - 1 )[ 0 ] );
  }

  @Test
  public void test_PDI_17765_MultipleSheets_Header_StartRowX() throws Exception {
    String firstResult = "24.0";
    String lastResult = "132.0";
    test_PDI_17765( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET, true, CNST_3_SHEET_START_ROW_ARRAY, CNST_3_ZERO_INT_ARRAY,
      CNST_3_SHEET_NAME_ARRAY );

    // Checks
    assertEquals( "Wrong first result", firstResult, rows.get( 0 )[ 0 ] );
    assertEquals( "Wrong last result", lastResult, rows.get( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET - 1 )[ 0 ] );
  }

  @Test
  public void test_PDI_17765_MultipleSheets_NoHeader_StartRowX() throws Exception {
    String firstResult = "23.0";
    String lastResult = "102.0";
    test_PDI_17765( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET, false, CNST_3_SHEET_START_ROW_ARRAY, CNST_3_ZERO_INT_ARRAY,
      CNST_3_SHEET_NAME_ARRAY );

    // Checks
    assertEquals( "Wrong first result", firstResult, rows.get( 0 )[ 0 ] );
    assertEquals( "Wrong last result", lastResult, rows.get( PDI_17765_ROW_LIMIT_MULTIPLE_SHEET - 1 )[ 0 ] );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_Header_InlinedStrings_Streaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( XLSX_FILE_WITH_INLINED_STRINGS );

    testReadBlankAndNullCells( true );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_Header_SharedStrings_Streaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( XLSX_FILE_WITH_SHARED_STRINGS );

    testReadBlankAndNullCells( true );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_Header_InlinedStrings_NoStreaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.POI );
    init( XLSX_FILE_WITH_INLINED_STRINGS );

    testReadBlankAndNullCells( true );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_Header_SharedStrings_NoStreaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.POI );
    init( XLSX_FILE_WITH_SHARED_STRINGS );

    testReadBlankAndNullCells( true );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_NoHeader_InlinedStrings_Streaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( XLSX_FILE_WITH_INLINED_STRINGS );

    testReadBlankAndNullCells( false );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_NoHeader_SharedStrings_Streaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.SAX_POI );
    init( XLSX_FILE_WITH_SHARED_STRINGS );

    testReadBlankAndNullCells( false );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_NoHeader_InlinedStrings_NoStreaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.POI );
    init( XLSX_FILE_WITH_INLINED_STRINGS );

    testReadBlankAndNullCells( false );
  }

  @Test
  public void testReadBlankAndNullCells_XSLSX_NoHeader_SharedStrings_NoStreaming() throws Exception {
    meta.setSpreadSheetType( SpreadSheetType.POI );
    init( XLSX_FILE_WITH_SHARED_STRINGS );

    testReadBlankAndNullCells( false );
  }

  /**
   * <p>Common code for the testReadBlankAndNullCells* tests.</p>
   *
   * @param startsWithHeader if the file has header row or not
   * @throws Exception if something went wrong
   */
  private void testReadBlankAndNullCells( boolean startsWithHeader ) throws Exception {
    meta.setStartsWithHeader( startsWithHeader );

    setFields( new ExcelInputField( "f1", -1, -1 ), new ExcelInputField( "f2", -1, -1 ),
      new ExcelInputField( "f3", -1, -1 ), new ExcelInputField( "f4", -1, -1 ) );

    process();

    // The full content of the file
    Object[][] allRows = new Object[][] {
      // Second cell is 'Blank' and third is 'null'
      { "val11", "", null, "val14" },
      // All cells have content
      { "val21", "val22", "val23", "val24" },
      // Second cell is 'Blank'
      { "val31", "", "val33", "val34" },
      // All cells have content
      { "val41", "val42", "val43", "val44" },
      // Second cell is 'null'
      { "val51", null, "val53", "val54" },
      // All cells have content
      { "val61", "val62", "val63", "val64" }
    };

    Object[][] expectedRows = allRows;

    if ( startsWithHeader ) {
      // When it has an Header, the first row won't show as content
      expectedRows = Arrays.copyOfRange( allRows, 1, allRows.length );
    }

    check( expectedRows );
  }
}
