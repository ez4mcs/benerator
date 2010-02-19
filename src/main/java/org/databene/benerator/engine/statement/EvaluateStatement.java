/*
 * (c) Copyright 2009-2010 by Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License.
 *
 * For redistributing this software or a derivative work under a license other
 * than the GPL-compatible Free Software License as defined by the Free
 * Software Foundation or approved by OSI, you must first obtain a commercial
 * license to this software product from Volker Bergmann.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.databene.benerator.engine.statement;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import org.databene.benerator.engine.BeneratorContext;
import org.databene.benerator.engine.Statement;
import org.databene.commons.Assert;
import org.databene.commons.BeanUtil;
import org.databene.commons.ConfigurationError;
import org.databene.commons.Context;
import org.databene.commons.ConversionException;
import org.databene.commons.ErrorHandler;
import org.databene.commons.Expression;
import org.databene.commons.IOUtil;
import org.databene.commons.LogCategories;
import org.databene.commons.ReaderLineIterator;
import org.databene.commons.ShellUtil;
import org.databene.commons.StringUtil;
import org.databene.commons.Level;
import org.databene.commons.SystemInfo;
import org.databene.commons.converter.LiteralParser;
import org.databene.commons.db.DBUtil;
import org.databene.commons.expression.ExpressionUtil;
import org.databene.platform.db.DBSystem;
import org.databene.script.Script;
import org.databene.script.ScriptUtil;
import org.databene.task.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes an &lt;evaluate/&gt; from an XML descriptor.<br/>
 * <br/>
 * Created at 23.07.2009 17:59:36
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class EvaluateStatement implements Statement {
	
	private static final Logger logger = LoggerFactory.getLogger(EvaluateStatement.class);
	
	private static final String SHELL = "shell";
	
	private static final Map<String, String> extensionMap;
	
	static {
		try {
			extensionMap = IOUtil.readProperties("org/databene/benerator/engine/statement/fileTypes.properties");
		} catch (IOException e) {
			throw new ConfigurationError(e);
		}
	}
	
	Expression<String> idEx;
	Expression<String> textEx;
	Expression<String> uriEx;
	Expression<String> typeEx;
	Expression<?> targetObjectEx;
	Expression<String> onErrorEx;
	Expression<String> encodingEx;
    Expression<Boolean> optimizeEx;
    Expression<?> assertionEx;

    public EvaluateStatement(Expression<String> idEx, Expression<String> textEx, 
    		Expression<String> uriEx, Expression<String> typeEx, Expression<?> targetObjectEx,
    		Expression<String> onErrorEx, Expression<String> encodingEx, Expression<Boolean> optimizeEx,
            Expression<?> assertionEx) {
    	this.idEx = idEx;
    	this.textEx = textEx;
    	this.uriEx = uriEx;
    	this.typeEx = typeEx;
    	this.targetObjectEx = targetObjectEx;
    	this.onErrorEx = onErrorEx;
    	this.encodingEx = encodingEx;
    	this.optimizeEx = optimizeEx;
    	this.assertionEx = assertionEx;
    }

	public void execute(BeneratorContext context) {
		try {
			String onErrorValue = ExpressionUtil.evaluate(onErrorEx, context);
			if (onErrorValue == null)
				onErrorValue = "fatal";
			
			String typeValue = ExpressionUtil.evaluate(typeEx, context);
			// if type is not defined, derive it from the file extension
			String uriValue = ExpressionUtil.evaluate(uriEx, context);
			if (typeEx == null && uriEx != null) {
				typeValue = mapExtensionOf(uriValue);
				if ("winshell".equals(typeValue))
					if (!SystemInfo.isWindows())
						throw new ConfigurationError("Need Windows to run file: " + uriValue);
					else
						typeValue = SHELL;
				else if ("unixshell".equals(typeValue))
					if (SystemInfo.isWindows())
						throw new ConfigurationError("Need Unix system to run file: " + uriValue);
					else
						typeValue = SHELL;
			}
			if (uriValue != null)
				uriValue = context.resolveRelativeUri(uriValue);
			Object targetObject = ExpressionUtil.evaluate(targetObjectEx, context);
			if (typeValue == null && targetObject instanceof DBSystem)
				typeValue = "sql";
			
            String textValue = ExpressionUtil.evaluate(textEx, context);
            String encoding = ExpressionUtil.evaluate(encodingEx, context);

			// run
			Object result = null;
			if ("sql".equals(typeValue)) {
	            result = runSql(uriValue, targetObject, onErrorValue, encoding, 
						textValue, optimizeEx.evaluate(context));
            } else if (SHELL.equals(typeValue)) {
				result = runShell(uriValue, textValue, onErrorValue);
            } else {
            	if (typeValue == null) 
            		typeValue = context.getDefaultScript();
				if (!StringUtil.isEmpty(uriValue))
					textValue = IOUtil.getContentOfURI(uriValue);
				result = runScript(textValue, typeValue, onErrorValue, context);
			}
			context.set("result", result);
			Object assertionValue = ExpressionUtil.evaluate(assertionEx, context);
			if (assertionValue instanceof String)
				assertionValue = LiteralParser.parse((String) assertionValue);
			if (assertionValue != null && !(assertionValue instanceof String && ((String) assertionValue).length() == 0)) {
				if (assertionValue instanceof Boolean) {
					if (!(Boolean) assertionValue)
						getErrorHandler(onErrorValue).handleError("Assertion failed: '" + assertionEx + "'");
				} else {
					if (!BeanUtil.equalsIgnoreType(assertionValue, result))
						getErrorHandler(onErrorValue).handleError("Assertion failed. Expected: '" + assertionValue + "', found: '" + result + "'");
				}
			}
			String idValue = ExpressionUtil.evaluate(idEx, context);
			if (idValue != null)
				context.set(idValue, result);
		} catch (ConversionException e) {
			throw new ConfigurationError(e);
		} catch (IOException e) {
			throw new ConfigurationError(e);
		}
    }

	private String mapExtensionOf(String uri) {
		String lcUri = uri.toLowerCase();
		for (Entry<String, String> entry : extensionMap.entrySet())
			if (lcUri.endsWith(entry.getKey()))
				return entry.getValue();
	    return null;
    }

	private ErrorHandler getErrorHandler(String level) {
	    ErrorHandler errorHandler = new ErrorHandler(getClass().getName(), Level.valueOf(level));
	    return errorHandler;
    }

	private Object runScript(String text, String type, String onError, Context context) {
		ErrorHandler errorHandler = new ErrorHandler(getClass().getName(),
				Level.valueOf(onError));
		try {
			Script script = ScriptUtil.parseScriptText(text, type);
			return script.evaluate(context);
		} catch (Exception e) {
			errorHandler.handleError("Error in script evaluation", e);
			return null;
		}
	}

	private int runShell(String uri, String text, String onError) {
		ErrorHandler errorHandler = new ErrorHandler(getClass().getName(),
				Level.valueOf(onError));
		if (text != null)
			return ShellUtil.runShellCommands(new ReaderLineIterator(
					new StringReader(text)), errorHandler);
		else if (uri != null)
			return ShellUtil.runShellCommand(uri, errorHandler);
		else
			throw new ConfigurationError(
					"At least uri or text must be provided in <execute>");
	}

	private Object runSql(String uri, Object targetObject, String onError,
			String encoding, String text, boolean optimize) {
		if (targetObject == null)
			throw new ConfigurationError("Please specify the 'target' database to execute the SQL script");
		Assert.instanceOf(targetObject, DBSystem.class, "target");
		DBSystem db = (DBSystem) targetObject;
		if (uri != null)
			logger.info("Executing script " + uri);
		else if (text != null)
			logger.info("Executing inline script");
		else
			throw new TaskException("No uri or content");
        Connection connection = null;
        Object result = null;
		ErrorHandler errorHandler = new ErrorHandler(LogCategories.SQL, Level.valueOf(onError));
        try {
            connection = db.createConnection();
            if (text != null)
            	result = DBUtil.runScript(text, connection, optimize, errorHandler);
            else
            	result = DBUtil.runScript(uri, encoding, connection, optimize, errorHandler);
            db.invalidate(); // possibly we changed the database structure
            connection.commit();
		} catch (Exception sqle) { 
            if (connection != null) {
            	try {
                    connection.rollback();
                } catch (SQLException e) {
                    // ignore this 2nd exception, we have other problems now (sqle)
                }
            }
            errorHandler.handleError("Error in SQL script execution", sqle);
		} finally {
            DBUtil.close(connection);
        }
		return result;
	}

}
