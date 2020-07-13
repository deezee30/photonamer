/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.util;

import java.io.Console;
import java.io.PrintStream;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Easy to use tool for quickly working with {@code String}s in
 * order to output them, modify them for use in printing and etc.
 *
 * @see Console
 * @see Formatter
 * @see PrintStream
 */
public final class Printer {

    /**
     * Used for separating large amounts of texts to simplify reading
     */
    public static final String BORDER = "---------------------------------------";

    private static final PrintStream DEFAULT_OUTPUT = System.out;

    private static boolean		debug			= false;
    private static String		prefix			= "PhotoNamer -> ";
    private static String		debugPrefix		= "[DEBUG] ";
    private static char			noPrefixChar	= 126; // char "~"
    private static PrintStream	output			= DEFAULT_OUTPUT;

    /**
     * Logs a message to the console if it's present using
     * {@link Console#format(String, Object...)}.  If a console is not
     * present nothing will be logged, although the formatted return string
     * will still be returned.
     *
     * <p>There will be a global prefix in front of the output text that
     * can be set using {@link #setPrefix(String)}.  By default, the prefix
     * is set to {@code EnhancedLib -{@literal >} }.  If for a reason the
     * programmer does <b>not</b> want to include the prefix before the
     * outputted string, they can add a defined {@link char} at the very
     * start, which can be set using {@link #setNoPrefixChar(char)}.
     *  The default {@link #noPrefixChar} {@code char} is by ID of {@code 126}
     * ({@code ~}).</p>
     *
     * <p>{@link Formatter} codes are available to be used in the
     * {@param string} with components being used in {@param components}.</p>
     *
     * @param 	string
     * 			String to be formatted with {@link Formatter}'s
     * 			default formatting layout.
     * @param 	components
     * 			Arguments referenced by the format specifiers in the format
     *          string.  If there are more arguments than format specifiers,
     *          the extra arguments are ignored.  The number of arguments is
     *          variable and may be zero.
     * @return	The already formatted string that has been outputted to
     * 			the console.  If {@link IllegalFormatException} was
     * 			thrown, the string returned remains unformatted.
     * @throws 	IllegalFormatException
     * 			If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.
     * @see 	Console#format(String, Object...)
     * @see		Console#printf(String, Object...)
     * @see		Formatter
     * @see 	#prefix(String, String)
     * @see		#setPrefix(String)
     * @see     #setNoPrefixChar(char)
     */
    public static Optional<String> log(String string,
                                       Object... components) {
        return write(output, prefix(string, prefix), components);
    }


    /**
     * Logs a message to the console if it's present and the provided check
     * is true using {@link Console#format(String , Object...)}.  If a
     * console is not present nothing will be logged.
     *
     * <p>There will be a global prefix in front of the output text that
     * can be set using {@link #setPrefix(String)}.  By default, the prefix
     * is set to {@code EnhancedLib -{@literal >} }.  If for a reason the
     * programmer does <b>not</b> want to include the prefix before the
     * outputted string, they can add a defined {@link char} at the very
     * start, which can be set using {@link #setNoPrefixChar(char)}.
     *  The default {@link #noPrefixChar} {@code char} is by ID of {@code 126}
     * ({@code ~}).</p>
     *
     * <p>{@link Formatter} codes are available to be used in the
     * {@param string} with components being used in {@param components}.</p>
     *
     * @param 	check
     * 			The boolean that needs to be checked in order for the message
     * 			to be outputted.  If {@code check == false}, nothing happen.
     * @param 	string
     * 			String to be formatted with {@link Formatter}'s
     *          default formatting layout.
     * @param 	components
     * 			Arguments referenced by the format specifiers in the format
     *          string.  If there are more arguments than format specifiers,
     *          the extra arguments are ignored.  The number of arguments is
     *          variable and may be zero.
     * @throws 	IllegalFormatException
     * 			If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.
     * @return 	The value of {@param check}.
     * @see     Console#printf(String , Object...)
     * @see 	Console#format(String , Object...)
     * @see     Formatter
     * @see 	#prefix(String , String)
     * @see		#setPrefix(String)
     * @see		#setNoPrefixChar(char)
     */
    public static boolean logIf(boolean check,
                                String string,
                                Object... components) {
        if (check) log(string, components);
        return check;
    }


    /**
     * <p>Debugs a message to the console if it's present and {@link
     * #enableDebugging(boolean)} is set to {@code true}.  This method works
     * like an ordinary {@link #log(String , Object...)} method would except
     * it only outputs the message to the console if debugging is enabled,
     * which should only be the case during testing as it can otherwise spam
     * the console with many useless material.  If a console is not present
     * nothing will be logged, although the formatted return string will
     * still be returned.  The prefix in front of the message output will be
     * a combination of a predetermined {@link #setDebugPrefix(String)}
     * {@code +} {@link #setPrefix(String)}.</p>
     *
     * <p>There will be a global prefix in front of the output text that can
     * be set using {@link #setPrefix(String)}.  By default, the prefix is
     * set to {@code EnhancedLib -{@literal >} }.  If for a reason the programmer
     * does <b>not</b> want to include the prefix before the outputted string,
     * they can add a defined {@link char} at the very start, which can be
     * set using {@link #setNoPrefixChar(char)}.  The default {@link #noPrefixChar}
     * {@code char} is by ID of {@code 126} ({@code ~}).</p>
     *
     * <p>{@link Formatter} codes are available to be used in the
     * {@param string} with components being used in {@param components}.</p>
     *
     * @param 	string
     * 			String to be formatted with {@link Formatter}'s
     *          default formatting layout.
     * @param 	components
     * 			Arguments referenced by the format specifiers in the format
     *          string.  If there are more arguments than format specifiers,
     *          the extra arguments are ignored.  The number of arguments is
     *          variable and may be zero.
     * @return 	The already formatted string that has been outputted to
     * 			the console.  If {@link IllegalFormatException} was
     * 			thrown, the string returned remains unformatted.
     * @throws 	IllegalFormatException
     * 			If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.
     * @see 	Console#format(String , Object...)
     * @see 	Console#printf(String , Object...)
     * @see     Formatter
     * @see 	#prefix(String , String)
     * @see 	#setPrefix(String)
     * @see     #setNoPrefixChar(char)
     * @see		#setDebugPrefix(String)
     * @see		#enableDebugging(boolean)
     */
    public static Optional<String> debug(String string,
                                         Object... components) {
        String msg = prefix(string, debugPrefix + prefix);
        return debug ? write(output, msg, components) : Optional.of(msg);
    }


    /**
     * <p>Debugs a message to the console if it's present, the provided check
     * is true using {@link Console#format(String , Object...)} and {@link
     * #enableDebugging(boolean)} is set to {@code true}.  If a console is
     * not present nothing will be logged.  This method works like an ordinary
     * {@link #logIf(boolean, String, Object...)} method would except it only
     * outputs the message to the console if debugging is enabled, which
     * should only be the case during testing as it can otherwise spam the
     * console with many useless material.  If a console is not present nothing
     * will be logged, although the formatted return string will still be
     * returned.  The prefix in front of the message output will be a combination
     * of a predetermined {@link #setDebugPrefix(String)} {@code +} {@link
     * #setPrefix(String)}.</p>
     *
     * <p>There will be a global prefix in front of the output text that can
     * be set using {@link #setPrefix(String)}.  By default, the prefix is
     * set to {@code EnhancedLib -{@literal >} }.  If for a reason the programmer
     * does <b>not</b> want to include the prefix before the outputted string,
     * they can add a defined {@link char} at the very start, which can be
     * set using {@link #setNoPrefixChar(char)}.  The default {@link #noPrefixChar}
     * {@code char} is by ID of {@code 126} ({@code ~}).</p>
     *
     * <p>{@link Formatter} codes are available to be used in the
     * {@param string} with components being used in {@param components}.</p>
     *
     * @param 	check
     * 			The boolean that needs to be checked in order for the message
     * 			to be outputted.  If {@code check == false}, nothing happen.
     * @param 	string
     * 			String to be formatted with {@link Formatter}'s
     *          default formatting layout.
     * @param 	components
     * 			Arguments referenced by the format specifiers in the format
     *          string.  If there are more arguments than format specifiers,
     *          the extra arguments are ignored.  The number of arguments is
     *          variable and may be zero.
     * @return 	The already formatted string that has been outputted to
     * 			the console.  If {@link IllegalFormatException} was
     * 			thrown, the string returned remains unformatted.
     * @throws 	IllegalFormatException
     * 			If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.
     * @see 	Console#format(String , Object...)
     * @see 	Console#printf(String , Object...)
     * @see     Formatter
     * @see 	#prefix(String , String)
     * @see     #setPrefix(String)
     * @see     #setNoPrefixChar(char)
     */
    public static boolean debugIf(boolean check,
                                  String string,
                                  Object... components) {
        if (check) debug(string, components);
        return check;
    }


    /**
     * @return 	Whether or not debugging is allowed, AKA value inputted for {@link
     * 			#enableDebugging(boolean)}.  By default the value is {@code false}.
     * @see     #setDebugPrefix(String)
     * @see     #debug(String , Object...)
     * @see     #debugIf(boolean , String , Object...)
     */
    public static boolean debugEnabled() {
        return debug;
    }


    /**
     * Turns debugging on and off.
     *
     * <p>Messages sent through {@link #debug(String,
     * Object...)} and {@link #debugIf(boolean, String, Object...)} are
     * only outputted when this is enabled, which console with many useless
     * material.</p>
     *
     * @param 	debug
     * 			Whether or not to enable debugging.
     * @return	The value of {@param debug}.
     * @see		#setDebugPrefix(String)
     * @see		#debug(String, Object...)
     * @see		#debugIf(boolean, String, Object...)
     */
    public static boolean enableDebugging(boolean debug) {
        log("Debugging has been %s", debug ? "enabled" : "disabled");
        return Printer.debug = debug;
    }


    /**
     * Sets a prefix that will be outputted before every message in the console
     * provided it is present.
     *
     * <p>By default, the prefix is set to {@code EnhancedLib -{@literal >} }.
     *  If for a reason the programmer does <b>not</b> want to include the
     * prefix before the outputted string, they can add a defined {@link char}
     * at the very start, which can be set using {@link #setNoPrefixChar(char)}.
     *  The default {@link #noPrefixChar} {@code char} is by ID of {@code 126}
     * ({@code ~}).</p>
     *
     * @param 	prefix
     * 			The string that will be placed before every outputted message.
     * @return	The value of {@param prefix}.
     * @see		#log(String, Object...)
     * @see		#setNoPrefixChar(char)
     */
    public static String setPrefix(String prefix) {
        return Printer.prefix = prefix;
    }


    /**
     * Sets a prefix that will be outputted before every <b>debugging</b>
     * message in the console provided it is present.
     *
     * <p>By default, the prefix is set to {@code [DEBUG] }.  If for a
     * reason the programmer does <b>not</b> want to include the prefix
     * before the outputted string, they can add a defined {@link char} at
     * the very start, which can be set using {@link #setNoPrefixChar(char)}.
     * The default {@link #noPrefixChar} {@code char} is by ID of {@code 126}
     * ({@code ~}).</p>
     *
     * @param 	debugPrefix
     * 			The string that will be placed before every outputted message.
     * @return 	The value of {@param prefix}.
     * @see     #debug(String , Object...)
     * @see     #setNoPrefixChar(char)
     */
    public static String setDebugPrefix(String debugPrefix) {
        return Printer.debugPrefix = debugPrefix;
    }


    /**
     * Sets the {@code char} that should be used before the outputting message
     * to indicate that the prefix should not be used.
     *
     * <p>If for a reason the programmer does <b>not</b> want to include the
     * prefix before the outputted string, they can add a defined {@link char}
     * at the very start, which can be set here.  The default {@link #noPrefixChar}
     * {@code char} is by ID of {@code 126} ({@code ~}).</p>
     *
     * @param 	noPrefixChar
     * 			{@code char} that should be used before the outputting message
     * 			to indicate that the prefix should not be used.
     * @return	The value of {@param noPrefixChar}.
     * @see		#prefix(String, String)
     * @see		#setPrefix(String)
     * @see		#log(String, Object...)
     */
    public static char setNoPrefixChar(char noPrefixChar) {
        return Printer.noPrefixChar = noPrefixChar;
    }


    /**
     * @return 	{@code char} that should be used before the outputting message
     * 			to indicate that the prefix should not be used.
     * @see 	#setNoPrefixChar(char)
     * @see		#prefix(String, String)
     * @see		#setNoPrefixChar(char)
     * @see 	#log(String, Object...)
     */
    public static char getNoPrefixChar() {
        return noPrefixChar;
    }


    public static String prefix(String string,
                                String prefix) {
        if (string == null) return null;
        notNull(prefix);

        boolean usePrefix = true;
        if (string.startsWith(String.valueOf(noPrefixChar))) {
            usePrefix = false;
            string = string.substring(1);
        }

        return (usePrefix ? prefix : "") + string;
    }


    public static PrintStream getOutput() {
        return output;
    }


    public static PrintStream setOutput(PrintStream output) {
        return Printer.output = output;
    }


    public static String buildMessage(String message,
                                      Object... components) {
        if (message != null) {
            // Replace null components with "null" string
            for (int x = 0; x < components.length; ++x) {
                final Object o = components[x];

                if (o == null) components[x] = "null";
            }

            try {
                message = String.format(message, components);
            } catch (IllegalFormatException ignored) {}
        }

        return message;
    }


    private static Optional<String> write(PrintStream output,
                                          String message,
                                          Object... components) {
        message = buildMessage(message, components);
        output.println(message);
        return Optional.of(message);
    }

    // Disable initialisation
    private Printer() {}
}