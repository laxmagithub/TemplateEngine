/*
 * Copyright (C) 2019 Dipl.-Inform. Kai Hofmann. All rights reserved!
 */
package de.powerstat.phplib.templateengine.test;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import de.powerstat.phplib.templateengine.TemplateEngine;


/**
 * TemplateEngine tests.
 */
public final class TemplateEngineTests
 {
  /**
   * Logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(TemplateEngineTests.class);


  /**
   * Default constructor.
   */
  public TemplateEngineTests()
   {
    super();
   }


  /**
   * Log template engine variables for debugging.
   *
   * @param engine TemplateEngine
   */
  private static void logVars(final TemplateEngine engine)
   {
    final String[] variables = engine.getVars();
    LOGGER.info("Number of variables: " + variables.length); //$NON-NLS-1$
    for (final String varname : variables)
     {
      LOGGER.info("variables: " + varname + " = " + engine.getVar(varname));  //$NON-NLS-1$//$NON-NLS-2$
     }
   }


  /**
   * Log undefined variables for debugging.
   *
   * @param engine TemplateEngine
   * @param varname Variable name for which get the undefined variables
   * @throws IOException IO exception
   */
  private static void logUndefVars(final TemplateEngine engine, final String varname) throws IOException
   {
    final List<String> undefinedVariables = engine.getUndefined(varname);
    LOGGER.info("length: " + undefinedVariables.size()); //$NON-NLS-1$
    for (final String undefVarname : undefinedVariables)
     {
      LOGGER.info("variables: " + undefVarname); //$NON-NLS-1$
     }
   }


  /**
   * Test default constructor.
   */
  @Test
  public void defaultConstructor()
   {
    final TemplateEngine engine = new TemplateEngine();
    assertNotNull(engine, "Default constructor failed!"); //$NON-NLS-1$
   }


  /**
   * Test read template file.
   */
  @Test
  public void readFile()
   {
    final TemplateEngine engine = new TemplateEngine();
    final boolean success = engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    assertTrue(success, "Template file template1.tmpl could not be loaded!"); //$NON-NLS-1$
   }


  /**
   * Test read template file.
   *
   * @throws IOException IO exception
   */
  @Test
  public void subst() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    final String variableValue = engine.subst("file1"); //$NON-NLS-1$
    LOGGER.debug("file1 = " + variableValue); //$NON-NLS-1$
    assertEquals("123\n{variable1}\n456\n", variableValue); //$NON-NLS-1$
   }


  /**
   * Get undefined variables from template.
   *
   * @throws IOException IO exception
   */
  @Test
  public void getUndefined() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* final String variableValue = */ engine.subst("file1"); //$NON-NLS-1$
    final List<String> undefinedVars = engine.getUndefined("file1"); //$NON-NLS-1$
    assertAll(
      () -> assertEquals(1, undefinedVars.size()),
      () -> assertEquals("variable1", undefinedVars.get(0)) //$NON-NLS-1$
    );
   }


  /**
   * Test set variable as empty.
   *
   * @throws IOException IO exception
   */
  @Test
  public void setVarEmpty() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable1"); //$NON-NLS-1$
    /* final String variableValue = */ engine.subst("file1"); //$NON-NLS-1$
    final List<String> undefinedVars = engine.getUndefined("file1"); //$NON-NLS-1$
    assertAll(
      () -> assertTrue(undefinedVars.isEmpty(), "Undefined variable(s) found!"), //$NON-NLS-1$
      () -> assertTrue(engine.getVar("variable1").isEmpty(), "variable1 is not empty!") //$NON-NLS-1$
    );
   }


  /**
   * Test set variable with value.
   *
   * @throws IOException IO exception
   */
  @Test
  public void setVar() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable1", "TEST");  //$NON-NLS-1$//$NON-NLS-2$
    /* final String variableValue = */ engine.subst("file1"); //$NON-NLS-1$
    final List<String> undefinedVars = engine.getUndefined("file1"); //$NON-NLS-1$
    assertAll(
      () -> assertTrue(undefinedVars.isEmpty(), "Undefined variable(s) found!"), //$NON-NLS-1$
      () -> assertEquals("TEST", engine.getVar("variable1")) //$NON-NLS-1$ //$NON-NLS-2$
    );
   }


  /**
   * Test get removed variable parsing.
   *
   * @throws IOException IO exception
   */
  @Test
  public void getRemoved() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    final String parseResult = engine.parse("output", "file1");  //$NON-NLS-1$//$NON-NLS-2$
    final String output = engine.get("output"); //$NON-NLS-1$
    assertAll(
      () -> assertNotNull(parseResult, "Parse result is null!"),
      () -> assertEquals("123\n\n456\n", output) //$NON-NLS-1$
    );
   }


  /**
   * Test get keep variable parsing.
   *
   * @throws IOException IO exception
   */
  @Test
  public void getKeep() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("keep"); //$NON-NLS-1$
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    /* String parseResult = */ engine.parse("output", "file1"); //$NON-NLS-1$ //$NON-NLS-2$
    final String output = engine.get("output"); //$NON-NLS-1$
    assertEquals("123\n{variable1}\n456\n", output); //$NON-NLS-1$
   }


  /**
   * Test get replace with comment variable parsing.
   *
   * @throws IOException IO exception
   */
  @Test
  public void getComment() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template1.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String subsrResult = */ engine.subst("file1"); //$NON-NLS-1$
    /* String parseResult = */ engine.parse("output", "file1");
    final String output = engine.get("output"); //$NON-NLS-1$
    assertEquals("123\n<!-- Template variable 'variable1' undefined -->\n456\n", output); //$NON-NLS-1$
   }


  /**
   * Test set non existing block.
   *
   * @throws IOException IO exception
   */
  @Test
  public void setBlockFailure() throws IOException
   {
    assertThrows(IllegalStateException.class, () ->
     {
      final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
      /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template2.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
      /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
      /* final boolean successBlock = */ engine.setBlock("file1", "blk2"); //$NON-NLS-1$ //$NON-NLS-2$
     }
    );
   }


  /**
   * Test set existing block.
   *
   * @throws IOException IO exception
   */
  @Test
  public void setBlock() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template2.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    final boolean successBlock = engine.setBlock("file1", "BLK1", "BLK1_BLK"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final String block = engine.getVar("BLK1"); //$NON-NLS-1$
    assertAll(
      () -> assertTrue(successBlock, "Block could not be cut out successfully!"), //$NON-NLS-1$
      () -> assertEquals("\n789\n{variable2}\nabc\n", block) //$NON-NLS-1$
    );
   }


  /**
   * Test parsing without append.
   *
   * @throws IOException IO exception
   */
  @Test
  public void parseNonAppend() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template2.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    engine.setVar("variable1", "TEST1"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable2", "TEST2"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable3", "TEST3"); //$NON-NLS-1$ //$NON-NLS-2$
    /* final boolean successBlock = */ engine.setBlock("file1", "BLK1", "BLK1_BLK"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /* String parseResult = */ engine.parse("BLK1_BLK", "BLK1", false); //$NON-NLS-1$ //$NON-NLS-2$
    /* String parseResult = */ engine.parse("BLK1_BLK", "BLK1", false); //$NON-NLS-1$ //$NON-NLS-2$
    assertEquals("\n789\nTEST2\nabc\n", engine.getVar("BLK1_BLK")); //$NON-NLS-1$ //$NON-NLS-2$
   }


  /**
   * Test parsing with append.
   *
   * @throws IOException IO exception
   */
  @Test
  public void parseAppend() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template2.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    engine.setVar("variable1", "TEST1"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable2", "TEST2"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable3", "TEST3"); //$NON-NLS-1$ //$NON-NLS-2$
    /* final boolean successBlock = */ engine.setBlock("file1", "BLK1", "BLK1_BLK"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /* String parseResult = */ engine.parse("BLK1_BLK", "BLK1", true); //$NON-NLS-1$ //$NON-NLS-2$
    /* String parseResult = */ engine.parse("BLK1_BLK", "BLK1", true); //$NON-NLS-1$ //$NON-NLS-2$
    assertEquals("\n789\nTEST2\nabc\n\n789\nTEST2\nabc\n", engine.getVar("BLK1_BLK")); //$NON-NLS-1$ //$NON-NLS-2$
   }


  /**
   * Test a specific parsing problem.
   *
   * Problem description for original phplib template engine:
   *
   * When using the same template-variable within a page and a block thats contained by the page,
   * then the behavior during parsing the page might be different.
   *
   * Thats because inside the subst() method a loop runs over the list of known template variables.
   *
   * When a variable has been replaced before a block (that contains the same variable) will be replaced,
   * then the variable will not be replaced within the block. When the block will be parsed first,
   * then the variable will also be replaced within the block.
   *
   * @throws IOException IO exception
   */
  @Test
  public void parseProblem() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("keep"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template3.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    /* String substResult = */ engine.subst("file1"); //$NON-NLS-1$
    engine.setVar("test0", "000"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("test1", "111"); //$NON-NLS-1$ //$NON-NLS-2$
    final boolean successBlock = engine.setBlock("file1", "BLK1"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("test2", "222"); //$NON-NLS-1$ //$NON-NLS-2$ // Wrong - better always define variables before doing a setBlock()
    final String output = engine.parse("output", "file1", false); //$NON-NLS-1$ //$NON-NLS-2$
    assertAll(
      () -> assertTrue(successBlock, "Could not cut out block!"), //$NON-NLS-1$
      () -> assertEquals("000 \n111 \n \nabc {test1} def {test2} ghi \n \n222 \n000 \n", output) //$NON-NLS-1$
    );

    // logVars(engine);
    // logUndefVars(engine, "file1");
   }


  /**
   * Test empty variable list.
   */
  @Test
  public void getVarsEmpty()
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    final String[] variables = engine.getVars();
    assertTrue(variables.length == 0, "Variables within empty template found!"); //$NON-NLS-1$
   }


  /**
   * Test variable list.
   */
  @Test
  public void getVars()
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    engine.setVar("variable1", "TEST"); //$NON-NLS-1$ //$NON-NLS-2$
    engine.setVar("variable2", "TEST"); //$NON-NLS-1$ //$NON-NLS-2$
    final String[] variables = engine.getVars();
    assertAll(
      () -> assertTrue(variables.length > 0, "No variables within template found!"), //$NON-NLS-1$
      () -> assertNotNull(variables[0], "No variable in list!")
    );
   }


  /**
   * Test subst empty file.
   *
   * @throws IOException IO exception
   */
  @Test
  public void substEmptyFile() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template4.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    final String result = engine.subst("file1"); //$NON-NLS-1$
    assertNull(result, "Template not empty!"); //$NON-NLS-1$
   }


  /**
   * Test set existing block.
   *
   * @throws IOException IO exception
   */
  @Test
  public void setBlockEmptyFile() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine("comment"); //$NON-NLS-1$
    /* final boolean successFile = */ engine.setFile("file1", new File("target/test-classes/template4.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    // /* final String result = */ engine.subst("file1"); //$NON-NLS-1$
    final boolean successBlock = engine.setBlock("file1", "BLK1", "BLK1_BLK"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertFalse(successBlock, "Block could be cut out successfully from empty template!"); //$NON-NLS-1$
   }


  /**
   * Get undefined variables from empty template.
   *
   * @throws IOException IO exception
   */
  @Test
  public void getUndefinedFromEmptyFile() throws IOException
   {
    final TemplateEngine engine = new TemplateEngine();
    /* final boolean success = */ engine.setFile("file1", new File("target/test-classes/template4.tmpl")); //$NON-NLS-1$ //$NON-NLS-2$
    // /* final String variableValue = */ engine.subst("file1"); //$NON-NLS-1$
    final List<String> undefinedVars = engine.getUndefined("file1"); //$NON-NLS-1$
    assertEquals(0, undefinedVars.size());
   }

 }