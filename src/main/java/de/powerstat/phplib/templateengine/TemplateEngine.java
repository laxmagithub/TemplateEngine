/*
 * Copyright (C) 2002-2003,2017-2020 Dipl.-Inform. Kai Hofmann. All rights reserved!
 */
package de.powerstat.phplib.templateengine;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * PHPLib compatible template engine.
 *
 * Unconditionally thread safe.
 * Not serializable, because serialization is dangerous, use Protocol Buffers or JSON instead!
 *
 * @author Kai Hofmann
 * @see <a href="https://sourceforge.net/projects/phplib/">https://sourceforge.net/projects/phplib/</a>
 * @see <a href="https://pear.php.net/package/HTML_Template_PHPLIB">https://pear.php.net/package/HTML_Template_PHPLIB</a>
 */
public final class TemplateEngine
 {
  /**
   * Logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(TemplateEngine.class);

  /**
   * Template name constant.
   */
  private static final String TEMPLATE = "template"; //$NON-NLS-1$

  /**
   * Varname name constant.
   */
  private static final String VARNAME = "varname"; //$NON-NLS-1$

  /**
   * Varname is empty error message constant.
   */
  private static final String VARNAME_IS_EMPTY = "varname is empty"; //$NON-NLS-1$

  /**
   * Varname is to long error message constant.
   */
  private static final String VARNAME_IS_TO_LONG = "varname is to long"; //$NON-NLS-1$

  /**
   * Varname does not match name pattern error message constant.
   */
  private static final String VARNAME_DOES_NOT_MATCH_NAME_PATTERN = "varname does not match name pattern"; //$NON-NLS-1$

  /**
   * Varname regexp pattern.
   */
  private static final String VARNAME_PATTERN = "^[a-zA-Z0-9_]{1,64}$"; //$NON-NLS-1$

  /**
   * Maximum template size.
   */
  private static final int MAX_TEMPLATE_SIZE = 1048576;

  /**
   * Maximum varname size.
   */
  private static final int MAX_VARNAME_SIZE = 64;

  /**
   * File name map.
   */
  private final Map<String, File> files = new ConcurrentHashMap<>();

  /**
   * Temporary variables map.
   */
  private final Map<String, String> tempVars = new ConcurrentHashMap<>();

  /**
   * Handling of undefined template variables.
   *
   * "remove"/0  =&gt; remove undefined variables
   * "keep"/1    =&gt; keep undefined variables
   * "comment"/2 =&gt; replace undefined variables with comments
   */
  private HandleUndefined unknowns = HandleUndefined.REMOVE; // final

  /**
   * Enum for handling of undefined variables.
   */
  public enum HandleUndefined
   {
    /**
     * Remove variables.
     */
    REMOVE(0),

    /**
     * Keep variables.
     */
    KEEP(1),

    /**
     * Change to XML comments.
     */
    COMMENT(2);


    /**
     * Action number.
     */
    private final int action;


    /**
     * Ordinal constructor.
     *
     * @param action Action number
     */
    HandleUndefined(final int action)
     {
      this.action = action;
     }


    /**
     * Get action number.
     *
     * @return Action number
     */
    public int getAction()
     {
      return this.action;
     }

   }


  /**
   * Copy constructor.
   *
   * @param engine Template engine
   * @throws NullPointerException If engine is null
   */
  public TemplateEngine(final TemplateEngine engine)
   {
    Objects.requireNonNull(engine, "engine"); //$NON-NLS-1$
    this.unknowns = engine.unknowns;
    for (final Map.Entry<String, String> entry : engine.tempVars.entrySet())
     {
      this.tempVars.put(entry.getKey(), entry.getValue());
     }
    for (final Map.Entry<String, File> entry : engine.files.entrySet())
     {
      this.files.put(entry.getKey(), entry.getValue());
     }
   }


  /**
   * Constructor.
   *
   * @param unknowns Handling of unknown template variables
   * @see HandleUndefined
   */
  public TemplateEngine(final HandleUndefined unknowns)
   {
    this.unknowns = unknowns;
   }


  /**
   * Default constructor - unknown variables will be handled as "remove", root is current directory.
   */
  public TemplateEngine()
   {
    this(HandleUndefined.REMOVE);
   }


  /**
   * Copy factory.
   *
   * @param engine TemplateEngine to copy
   * @return A new TemplateEngine instance that is a copy of engine.
   * @throws NullPointerException If engine is null
   */
  public static TemplateEngine newInstance(final TemplateEngine engine)
   {
    return new TemplateEngine(engine);
   }


  /**
   * Get new instance from a UTF-8 encoded text file.
   *
   * @param file Text file (UTF-8 encoded) to load as template
   * @return A new TemplateEngine instance where the template variable name is 'template'
   * @throws FileNotFoundException When the given file does not exist
   * @throws IOException When the given file is to large
   * @throws IllegalArgumentException When the given file is null
   * @throws NullPointerException If file is null
   */
  public static TemplateEngine newInstance(final File file) throws IOException
   {
    Objects.requireNonNull(file, "file"); //$NON-NLS-1$
    if (!file.isFile())
     {
      if (!file.exists())
       {
        throw new FileNotFoundException(file.getAbsolutePath());
       }
      // Load all files (*.tmpl) from directory?
      throw new AssertionError(file.getAbsolutePath() + " is a directory and not a file!"); //$NON-NLS-1$
     }
    if (file.length() > MAX_TEMPLATE_SIZE)
     {
      throw new IOException("file to large: " + file.length()); //$NON-NLS-1$
     }
    final TemplateEngine templ = new TemplateEngine();
    /*
    String filename = file.getName();
    final int extPos = filename.lastIndexOf('.');
    if (extPos > -1)
     {
      filename = filename.substring(0, extPos);
     }
    filename = filename.toLowerCase(Locale.getDefault());
    templ.setFile(filename, file);
    */
    templ.setFile(TEMPLATE, file);
    return templ;
   }


  /**
   * Get new instance from a stream.
   *
   * @param stream UTF-8 stream to read the template from
   * @return A new TemplateEngine instance where the template variable name is 'template'.
   * @throws IOException If an I/O error occurs
   * @throws IllegalArgumentException When the given stream is null
   * @throws IllegalStateException If the stream is empty
   * @throws NullPointerException If stream is null
   */
  public static TemplateEngine newInstance(final InputStream stream) throws IOException
   {
    Objects.requireNonNull(stream, "stream"); //$NON-NLS-1$
    final StringBuilder fileBuffer = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
     {
      String line = reader.readLine();
      while (line != null)
       {
        fileBuffer.append(line);
        fileBuffer.append('\n');
        line = reader.readLine();
       }
     }
    if (fileBuffer.length() == 0)
     {
      throw new IllegalStateException("Empty stream"); //$NON-NLS-1$
     }
    final TemplateEngine templ = new TemplateEngine();
    templ.setVar(TEMPLATE, fileBuffer.toString());
    return templ;
   }


  /**
   * Get new instance from a string.
   *
   * @param template Template string
   * @return A new TemplateEngine instance where the template variable name is 'template'.
   * @throws IllegalArgumentException When the given string is null or empty
   * @throws NullPointerException If template is null
   */
  public static TemplateEngine newInstance(final String template)
   {
    Objects.requireNonNull(template, TEMPLATE);
    if (template.isEmpty())
     {
      throw new IllegalArgumentException("template is empty"); //$NON-NLS-1$
     }
    if (template.length() > MAX_TEMPLATE_SIZE)
     {
      throw new IllegalArgumentException("template to large"); //$NON-NLS-1$
     }
    // if (!template.matches("^.+$"))
    final TemplateEngine templ = new TemplateEngine();
    templ.setVar(TEMPLATE, template);
    return templ;
   }


  /**
   * Handling of unknown template variables during parsing.
   *
   * @param newUnknowns How to handle unknown variables.
   * @see HandleUndefined
   * @deprecated Use TemplateEngine(final HandleUndefined unknowns) instead
   */
  @Deprecated(since = "1.4", forRemoval = false)
  public void setUnknowns(final HandleUndefined newUnknowns)
   {
    this.unknowns = newUnknowns;
   }


  /**
   * Set template file for variable.
   *
   * @param newVarname Variable that should hold the template
   * @param newFile Template file UTF-8 encoded
   * @return true when successful (file exists) otherwise false
   * @throws NullPointerException If newVarname or newFile is null
   * @throws IllegalArgumentException If newVarname is empty
   */
  public boolean setFile(final String newVarname, final File newFile)
   {
    Objects.requireNonNull(newVarname, "newVarname"); //$NON-NLS-1$
    Objects.requireNonNull(newFile, "newFile"); //$NON-NLS-1$
    if (newVarname.isEmpty())
     {
      throw new IllegalArgumentException("newVarname is empty"); //$NON-NLS-1$
     }
    if (newVarname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException("newVarname is to long"); //$NON-NLS-1$
     }
    if (!newVarname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException("newVarname does not match name pattern"); //$NON-NLS-1$
     }
    boolean exists = newFile.exists();
    if (exists)
     {
      if (newFile.length() > MAX_TEMPLATE_SIZE)
       {
        throw new IllegalArgumentException("newFile to large"); //$NON-NLS-1$
       }
      this.files.put(newVarname, newFile);
     }
    else
     {
      try (InputStream stream = this.getClass().getResourceAsStream("/" + newFile.getName())) //$NON-NLS-1$
       {
        if (stream != null)
         {
          exists = true;
          this.files.put(newVarname, newFile);
         }
       }
      catch (final IOException ignored)
        {
         // exists is already false
         if (LOGGER.isWarnEnabled())
          {
           LOGGER.warn("File does not exist: " + newFile.getAbsolutePath(), ignored); //$NON-NLS-1$
          }
        }
     }
    return exists;
   }


  /**
   * Load template file (UTF-8 encoded) if required.
   *
   * @param varname Variable to read from file
   * @return true if successful otherwise false
   * @throws FileNotFoundException File not found
   * @throws IOException IO exception
   */
  private boolean loadfile(final String varname) throws IOException
   {
    assert (varname != null) && !varname.isEmpty() && (varname.length() <= MAX_VARNAME_SIZE);
    if (this.tempVars.containsKey(varname)) // Already loaded?
     {
      return true;
     }
    final File file = this.files.get(varname);
    if (file == null)
     {
      return false;
     }
    InputStream istream = this.getClass().getResourceAsStream("/" + file.getName()); //$NON-NLS-1$ // Read from classpath/jar
    if (istream == null)
     {
      istream = Files.newInputStream(this.files.get(varname).toPath(), StandardOpenOption.READ); // Read from filesystem
     }
    final StringBuilder fileBuffer = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(istream, StandardCharsets.UTF_8)))
     {
      String line = reader.readLine();
      while (line != null)
       {
        fileBuffer.append(line);
        fileBuffer.append('\n');
        line = reader.readLine();
       }
     }
    if (fileBuffer.length() == 0)
     {
      return false;
     }
    setVar(varname, fileBuffer.toString());
    return true;
   }


  /**
   * Get template variable value.
   *
   * @param varname Template variable name
   * @return Template variables value
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public String getVar(final String varname)
   {
    Objects.requireNonNull(varname, VARNAME);
    if (varname.isEmpty())
     {
      throw new IllegalArgumentException(VARNAME_IS_EMPTY);
     }
    if (varname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException(VARNAME_IS_TO_LONG);
     }
    if (!varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException(VARNAME_DOES_NOT_MATCH_NAME_PATTERN);
     }
    final String value = this.tempVars.get(varname);
    return (value == null) ? "" : value; //$NON-NLS-1$
   }


  /**
   * Set template variables value.
   *
   * @param varname Template variable name
   * @param value Template variable value, could  be null
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public void setVar(final String varname, final String value)
   {
    Objects.requireNonNull(varname, VARNAME);
    if (varname.isEmpty())
     {
      throw new IllegalArgumentException(VARNAME_IS_EMPTY);
     }
    if (varname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException(VARNAME_IS_TO_LONG);
     }
    if ((value != null) && (value.length() > MAX_TEMPLATE_SIZE))
     {
      throw new IllegalArgumentException("value is to large"); //$NON-NLS-1$
     }
    if (!varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException(VARNAME_DOES_NOT_MATCH_NAME_PATTERN);
     }
    // if (!value.matches("^.+$"))
    this.tempVars.put(varname, (value == null) ? "" : value); //$NON-NLS-1$
   }


  /**
   * Set template variable as empty.
   *
   * @param varname Template variable name
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public void setVar(final String varname)
   {
    setVar(varname, ""); //$NON-NLS-1$
   }


  /**
   * Unset template variable.
   *
   * @param varname Template variable name
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public void unsetVar(final String varname)
   {
    Objects.requireNonNull(varname, VARNAME);
    if (varname.isEmpty())
     {
      throw new IllegalArgumentException(VARNAME_IS_EMPTY);
     }
    if (varname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException(VARNAME_IS_TO_LONG);
     }
    if (!varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException(VARNAME_DOES_NOT_MATCH_NAME_PATTERN);
     }
    /* String value = */ this.tempVars.remove(varname);
   }


  /**
   * Set template block (cut it from parent template and replace it with a variable).
   *
   * Used for repeatable blocks
   *
   * @param parent Name of parent template variable
   * @param varname Name of template block
   * @param name Name of variable in which the block will be placed - if empty will be the same as varname
   * @return true on sucess otherwise false
   * @throws IOException IO exception
   * @throws IllegalStateException When no block with varname is found.
   * @throws NullPointerException If parent or varname is null
   * @throws IllegalArgumentException If parent or varname is empty
   */
  public boolean setBlock(final String parent, final String varname, final String name) throws IOException
   {
    Objects.requireNonNull(parent, "parent"); //$NON-NLS-1$
    Objects.requireNonNull(varname, VARNAME);
    Objects.requireNonNull(name, "name"); //$NON-NLS-1$
    if (parent.isEmpty() || varname.isEmpty())
     {
      throw new IllegalArgumentException("parent or varname is empty"); //$NON-NLS-1$
     }
    if ((parent.length() > MAX_VARNAME_SIZE) || (varname.length() > MAX_VARNAME_SIZE) || (name.length() > MAX_VARNAME_SIZE))
     {
      throw new IllegalArgumentException("parent, varname or name is to long"); //$NON-NLS-1$
     }
    if (!parent.matches(VARNAME_PATTERN) || !varname.matches(VARNAME_PATTERN) || (!name.isEmpty() && (!name.matches(VARNAME_PATTERN))))
     {
      throw new IllegalArgumentException("parent, varname or name does not match name pattern"); //$NON-NLS-1$
     }
    if (!loadfile(parent))
     {
      return false;
     }
    String internName = name;
    if ("".equals(internName)) //$NON-NLS-1$
     {
      internName = varname;
     }
    final Pattern pattern = Pattern.compile("<!--\\s+BEGIN " + varname + "\\s+-->(.*)<!--\\s+END " + varname + "\\s+-->", Pattern.DOTALL | Pattern.MULTILINE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final Matcher matcher = pattern.matcher(getVar(parent));
    final String str = matcher.replaceFirst("{" + internName + "}"); //$NON-NLS-1$ //$NON-NLS-2$
    setVar(varname, matcher.group(1));
    setVar(parent, str);
    return true;
   }


  /**
   * Set template block (cut it from parent template and replace it with a variable).
   *
   * Used for on/off blocks
   *
   * @param parent Name of parent template variable
   * @param varname Name of template block
   * @return true on sucess otherwise false
   * @throws IOException IO exception
   * @throws IllegalStateException When no block with varname is found.
   * @throws NullPointerException If parent or varname is null
   * @throws IllegalArgumentException If parent or varname is empty
   */
  public boolean setBlock(final String parent, final String varname) throws IOException
   {
    return setBlock(parent, varname, ""); //$NON-NLS-1$
   }


  /**
   * Replace variables old version.
   *
   * @param block Template block
   * @return Template/Block with replaced variables
   */
  /*
  private String replaceVarsOld(String block)
   {
    assert (block != null) && !block.isEmpty();
    // loop over all known variables an replace them
    if (!this.tempVars.isEmpty())
     {
      final Set<Entry<String, String>> tempVarsSet = this.tempVars.entrySet();
      final Iterator<Entry<String, String>> iter = tempVarsSet.iterator();
      while (iter.hasNext())
       {
        final Map.Entry<String, String> mapEntry = iter.next();
        // convert into regexp (special char filter)
        block = Pattern.compile("\\{" + mapEntry.getKey() + "\\}").matcher(block).replaceAll(mapEntry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
       }
     }
    return block;
   }
  */


  /**
   * Replace variables new version.
   *
   * @param block Template block
   * @return Template/Block with replaced variables
   */
  private String replaceVarsNew(final String block)
   {
    assert (block != null) && !block.isEmpty() && (block.length() < MAX_TEMPLATE_SIZE);
    // assert block.matches("^.+$")
    // Get variable names to replace from varname
    final Matcher matcherTemplate = Pattern.compile("\\{([^{^}\n\r\t :]+)\\}").matcher(block); //$NON-NLS-1$
    final Set<String> varsSetTemplate = new TreeSet<>();
    while (matcherTemplate.find())
     {
      final String varnameTemplate = block.substring(matcherTemplate.start() + 1, matcherTemplate.end() - 1);
      if (this.tempVars.containsKey(varnameTemplate))
       {
        varsSetTemplate.add(varnameTemplate);
       }
     }
    String resBlock = block;
    for (final String varName : varsSetTemplate)
     {
      resBlock = resBlock.replaceAll("\\{" + varName + "\\}", getVar(varName)); //$NON-NLS-1$ //$NON-NLS-2$
     }
    return resBlock;
   }


  /**
   * Substitute variable with its content.
   *
   * @param varname Variable name
   * @return Replaced variable content or empty string
   * @throws IOException File not found or IO exception
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public String subst(final String varname) throws IOException
   {
    Objects.requireNonNull(varname, VARNAME);
    if (varname.isEmpty())
     {
      throw new IllegalArgumentException(VARNAME_IS_EMPTY);
     }
    if (varname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException(VARNAME_IS_TO_LONG);
     }
    if (!varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException(VARNAME_DOES_NOT_MATCH_NAME_PATTERN);
     }
    if (!loadfile(varname))
     {
      return ""; //$NON-NLS-1$
     }
    // return replaceVarsOld(getVar(varname));
    return replaceVarsNew(getVar(varname));
   }


  /**
   * Parse a variable and replace all variables within it by their content.
   *
   * @param target Target for parsing operation
   * @param varname Parse the content of this variable
   * @param append true for appending blocks to target, otherwise false for replacing targets content
   * @return Variables content after parsing
   * @throws IOException File not found or IO exception
   * @throws NullPointerException If target or varname is null
   * @throws IllegalArgumentException If target or varname is empty
   */
  public String parse(final String target, final String varname, final boolean append) throws IOException
   {
    Objects.requireNonNull(target, "target"); //$NON-NLS-1$
    Objects.requireNonNull(varname, VARNAME);
    if (target.isEmpty() || varname.isEmpty())
     {
      throw new IllegalArgumentException("target or varname is empty"); //$NON-NLS-1$
     }
    if ((target.length() > MAX_VARNAME_SIZE) || (varname.length() > MAX_VARNAME_SIZE))
     {
      throw new IllegalArgumentException("target or varname is to long"); //$NON-NLS-1$
     }
    if (!target.matches(VARNAME_PATTERN) || !varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException("target or varname does not match name pattern"); //$NON-NLS-1$
     }
    final String str = subst(varname);
    setVar(target, (append ? getVar(target) : "") + str); //$NON-NLS-1$
    return str;
   }


  /**
   * Parse a variable and replace all variables within it by their content.
   *
   * Don't append
   *
   * @param target Target for parsing operation
   * @param varname Parse the content of this variable
   * @return Variables content after parsing
   * @throws IOException File not found or IO exception
   * @throws NullPointerException If target or varname is null
   * @throws IllegalArgumentException If target or varname is empty
   */
  public String parse(final String target, final String varname) throws IOException
   {
    return parse(target, varname, false);
   }


  /**
   * Get list of all template variables.
   *
   * @return Array with names of template variables
   */
  public List<String> getVars()
   {
    if (this.tempVars.isEmpty())
     {
      return Collections.emptyList();
     }
    final List<String> result = new ArrayList<>();
    for (final Entry<String, String> entry : this.tempVars.entrySet())
     {
      result.add(entry.getKey()); // entry.getValue();
     }
    return Collections.unmodifiableList(result);
   }


  /**
   * Get list with all undefined template variables.
   *
   * @param varname Variable to parse for undefined variables
   * @return List with undefined template variables names
   * @throws IOException  File not found or IO exception
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public List<String> getUndefined(final String varname) throws IOException
   {
    Objects.requireNonNull(varname, VARNAME);
    if (varname.isEmpty())
     {
      throw new IllegalArgumentException(VARNAME_IS_EMPTY);
     }
    if (varname.length() > MAX_VARNAME_SIZE)
     {
      throw new IllegalArgumentException(VARNAME_IS_TO_LONG);
     }
    if (!varname.matches(VARNAME_PATTERN))
     {
      throw new IllegalArgumentException(VARNAME_DOES_NOT_MATCH_NAME_PATTERN);
     }
    if (!loadfile(varname))
     {
      return Collections.emptyList();
     }
    final Pattern pattern = Pattern.compile("\\{([^ \\t\\r\\n}]+)\\}"); //$NON-NLS-1$
    final Matcher matcher = pattern.matcher(getVar(varname));
    boolean result = matcher.find();
    final List<String> undefvars = new ArrayList<>();
    while (result)
     {
      final String vname = matcher.group(1);
      if (!this.tempVars.containsKey(vname) && !undefvars.contains(vname))
       {
        undefvars.add(vname);
       }
      result = matcher.find();
     }
    return Collections.unmodifiableList(undefvars);
   }


  /**
   * Handle undefined template variables after parsing has happened.
   *
   * @param template Template to parse for unknown variables
   * @return Modified template as specified by the "unknowns" setting
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public String finish(final String template)
   {
    Objects.requireNonNull(template, TEMPLATE);
    if (template.isEmpty())
     {
      throw new IllegalArgumentException("template is empty"); //$NON-NLS-1$
     }
    if (template.length() > MAX_TEMPLATE_SIZE)
     {
      throw new IllegalArgumentException("template is to large"); //$NON-NLS-1$
     }
    // if (!template.matches("^.+$"))
    String result = template;
    final Pattern pattern = Pattern.compile("\\{([^ \\t\\r\\n}]+)\\}"); //$NON-NLS-1$
    final Matcher matcher = pattern.matcher(result);
    switch (this.unknowns)
     {
      case KEEP:
        break;
      case REMOVE:
        result = matcher.replaceAll(""); //$NON-NLS-1$
        break;
      case COMMENT:
        result = matcher.replaceAll("<!-- Template variable '$1' undefined -->"); //$NON-NLS-1$
        break;
      default: // For the case that enum HandleUndefined will be extended!
        throw new AssertionError(this.unknowns);
     }
    return result;
   }


  /**
   * Shortcut for finish(getVar(varname)).
   *
   * @param varname Name of template variable
   * @return Value of template variable
   * @throws NullPointerException If varname is null
   * @throws IllegalArgumentException If varname is empty
   */
  public String get(final String varname)
   {
    return finish(getVar(varname));
   }


  /**
   * Returns the string representation of this TemplatEngine.
   *
   * The exact details of this representation are unspecified and subject to change, but the following may be regarded as typical:
   *
   * "TemplateEngine[unknowns=REMOVE, vars=[name, ...]]"
   *
   * @return String representation of this TemplatEngine.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
   {
    return new StringBuilder().append("TemplateEngine[unknowns=").append(this.unknowns).append(", files=").append(this.files.values().stream().map(File::getName).reduce((s1, s2) -> s1 + ", " + s2)).append(", vars=").append(getVars()).append("]").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
   }


  /**
   * Calculate hash code.
   *
   * @return Hash
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
   {
    return Objects.hash(this.unknowns, this.files, this.tempVars);
   }


  /**
   * Is equal with another object.
   *
   * @param obj Object
   * @return true when equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
   {
    if (this == obj)
     {
      return true;
     }
    if (!(obj instanceof TemplateEngine))
     {
      return false;
     }
    final TemplateEngine other = (TemplateEngine)obj;
    return (this.unknowns == other.unknowns) && this.files.equals(other.files) && this.tempVars.equals(other.tempVars);
   }

 }
