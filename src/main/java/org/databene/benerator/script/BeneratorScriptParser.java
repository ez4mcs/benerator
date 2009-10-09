/*
 * (c) Copyright 2009 by Volker Bergmann. All rights reserved.
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

package org.databene.benerator.script;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.databene.commons.ArrayFormat;
import org.databene.commons.Assert;
import org.databene.commons.BeanUtil;
import org.databene.commons.Context;
import org.databene.commons.Expression;
import org.databene.commons.bean.DefaultClassProvider;
import org.databene.commons.converter.AnyConverter;
import org.databene.commons.expression.BinaryExpression;
import org.databene.commons.expression.ConstantExpression;
import org.databene.commons.expression.UnaryExpression;
import org.databene.model.data.PrimitiveType;
import org.hsqldb.lib.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses Benerator Script statements and converts expressions and statements to Java objects.<br/>
 * <br/>
 * Created at 05.10.2009 18:52:31
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class BeneratorScriptParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeneratorScriptParser.class);
	private static final Expression<?>[] EMPTY_ARGUMENT_LIST = {};

    // interface -------------------------------------------------------------------------------------------------------

    public static Expression<?> parseExpression(String text) throws ParseException {
        if (StringUtil.isEmpty(text))
            return null;
        try {
        	BeneratorLexer lex = new BeneratorLexer(new ANTLRReaderStream(new StringReader(text)));
	        CommonTokenStream tokens = new CommonTokenStream(lex);
	        BeneratorParser parser = new BeneratorParser(tokens);
	        BeneratorParser.expression_return r = parser.expression();
	        if (parser.getNumberOfSyntaxErrors() > 0)
	        	throw new ParseException("Illegal regex: " + text, -1);
	        if (r != null) {
	        	CommonTree tree = (CommonTree) r.getTree();
	        	if (LOGGER.isDebugEnabled())
	        		LOGGER.debug("parsed " + text + " to " + tree.toStringTree());
	            return convertNode(tree);
	        } else
	        	return null;
        } catch (RuntimeException e) {
        	if (e.getCause() instanceof RecognitionException)
        		throw mapToParseException((RecognitionException) e.getCause());
        	else
        		throw e;
        } catch (IOException e) {
        	throw new IllegalStateException("Encountered illegal state in regex parsing", e);
        } catch (RecognitionException e) {
        	throw mapToParseException(e);
        }
    }
	
    public static Expression<?>[] parseBeanSpecList(String text) throws ParseException {
        if (StringUtil.isEmpty(text))
            return null;
        try {
        	BeneratorLexer lex = new BeneratorLexer(new ANTLRReaderStream(new StringReader(text)));
	        CommonTokenStream tokens = new CommonTokenStream(lex);
	        BeneratorParser parser = new BeneratorParser(tokens);
	        BeneratorParser.beanSpecList_return r = parser.beanSpecList();
	        if (parser.getNumberOfSyntaxErrors() > 0)
	        	throw new ParseException("Illegal regex: " + text, -1);
	        if (r != null) {
	        	CommonTree tree = (CommonTree) r.getTree();
	        	if (LOGGER.isDebugEnabled())
	        		LOGGER.debug("parsed " + text + " to " + tree.toStringTree());
	            return convertBeanSpecList(tree);
	        } else
	        	return null;
        } catch (RuntimeException e) {
        	if (e.getCause() instanceof RecognitionException)
        		throw mapToParseException((RecognitionException) e.getCause());
        	else
        		throw e;
        } catch (IOException e) {
        	throw new IllegalStateException("Encountered illegal state in regex parsing", e);
        } catch (RecognitionException e) {
        	throw mapToParseException(e);
        }
    }
	
    private static Expression<?>[] convertBeanSpecList(CommonTree node) throws ParseException {
	    int childCount = node.getChildCount();
	    Expression<?>[] specs = new Expression<?>[childCount];
	    for (int i = 0; i < childCount; i++)
	    	specs[i] = convertBeanSpec(childAt(i, node));
	    return specs;
    }

	public static Expression<?> parseBeanSpec(String text) throws ParseException {
        if (StringUtil.isEmpty(text))
            return null;
        try {
        	BeneratorLexer lex = new BeneratorLexer(new ANTLRReaderStream(new StringReader(text)));
	        CommonTokenStream tokens = new CommonTokenStream(lex);
	        BeneratorParser parser = new BeneratorParser(tokens);
	        BeneratorParser.beanSpec_return r = parser.beanSpec();
	        if (parser.getNumberOfSyntaxErrors() > 0)
	        	throw new ParseException("Illegal regex: " + text, -1);
	        if (r != null) {
	        	CommonTree tree = (CommonTree) r.getTree();
	        	if (LOGGER.isDebugEnabled())
	        		LOGGER.debug("parsed " + text + " to " + tree.toStringTree());
	        	return convertBeanSpec(tree);
	        } else
	        	return null;
        } catch (RuntimeException e) {
        	if (e.getCause() instanceof RecognitionException)
        		throw mapToParseException((RecognitionException) e.getCause());
        	else
        		throw e;
        } catch (IOException e) {
        	throw new IllegalStateException("Encountered illegal state in regex parsing", e);
        } catch (RecognitionException e) {
        	throw mapToParseException(e);
        }
    }
	
    private static ParseException mapToParseException(RecognitionException e) {
    	return new ParseException("Error parsing Benerator expression: " + e.getMessage(), e.charPositionInLine);
    }

	private static Expression<?> convertBeanSpec(CommonTree node) throws ParseException {
		if (node.getType() == BeneratorLexer.QUALIFIEDNAME)
			return new QNBeanSpecExpression(convertQualifiedNameToStringArray(node));
		else
			return convertNode(node);
	}

	private static Expression<?> convertNode(CommonTree node) throws ParseException {
    	switch (node.getType()) {
			case BeneratorLexer.NULL: return new ConstantExpression<Object>(null);
			case BeneratorLexer.BOOLEANLITERAL: return convertBooleanLiteral(node);
			case BeneratorLexer.INTLITERAL: return convertIntLiteral(node);
			case BeneratorLexer.DECIMALLITERAL: return convertDecimalLiteral(node);
    		case BeneratorLexer.STRINGLITERAL: return convertStringLiteral(node);
    		case BeneratorLexer.IDENTIFIER: return convertIdentifier(node);
    		case BeneratorLexer.QUALIFIEDNAME: return convertQualifiedName(node);
    		case BeneratorLexer.TYPE: return convertType(node);
			case BeneratorLexer.CONSTRUCTOR: return convertCreator(node);
			case BeneratorLexer.BEAN: return convertBean(node);
    		case BeneratorLexer.INVOCATION: return convertInvocation(node);
    		case BeneratorLexer.SUBINVOCATION: return convertSubInvocation(node);
			case BeneratorLexer.INDEX: return convertIndex(node);
			case BeneratorLexer.FIELD: return convertField(node);
			case BeneratorLexer.CAST: return convertCast(node);
			case BeneratorLexer.NEGATION: return convertNegation(node);
			case BeneratorLexer.BANG: return convertLogicalComplement(node);
			case BeneratorLexer.TILDE: return convertBitwiseComplement(node);
			case BeneratorLexer.PLUS: return convertPlus(node);
			case BeneratorLexer.SUB: return convertMinus(node);
			case BeneratorLexer.STAR: return convertStar(node);
			case BeneratorLexer.SLASH: return convertSlash(node);
			case BeneratorLexer.PERCENT: return convertPercent(node);
			case BeneratorLexer.AMP: return convertAnd(node);
			case BeneratorLexer.BAR: return convertInclusiveOr(node);
			case BeneratorLexer.CARET: return convertExclusiveOr(node);
			case BeneratorLexer.EQEQ: return convertEquals(node);
			case BeneratorLexer.BANGEQ: return convertNotEquals(node);
			case BeneratorLexer.LT: return convertLess(node);
			case BeneratorLexer.LE: return convertLessOrEquals(node);
			case BeneratorLexer.GT: return convertGreater(node);
			case BeneratorLexer.GE: return convertGreaterOrEquals(node);
			case BeneratorLexer.SHIFT_LEFT: return convertShiftLeft(node);
			case BeneratorLexer.SHIFT_RIGHT: return convertShiftRight(node);
			case BeneratorLexer.SHIFT_RIGHT2: return convertShiftRight2(node);
			case BeneratorLexer.AMPAMP: return convertConditionalAnd(node);
			case BeneratorLexer.BARBAR: return convertConditionalOr(node);
			case BeneratorLexer.QUES: return convertConditionalExpression(node);
			default: throw new ParseException("Unknown token type: " + node.getToken(), node.getCharPositionInLine());
    	}
    }

	private static Expression<Boolean> convertBooleanLiteral(CommonTree node) {
		return new ConstantExpression<Boolean>(Boolean.parseBoolean(node.getText()));
    }

    private static Expression<String> convertStringLiteral(CommonTree node) {
		String rawString = node.getText();
		return new ConstantExpression<String>(rawString.substring(1, rawString.length() - 1));
    }

    private static Expression<String> convertIdentifier(CommonTree node) {
		return new ConstantExpression<String>(node.getText());
    }

    /** 
     * This intended for top-level QNs; for QNs that are part of another expression, 
     * use specific methods like {@link #convertQualifiedNameToStringArray(CommonTree)}. 
     */
    private static Expression<?> convertQualifiedName(CommonTree node) {
    	return new QNExpression(convertQualifiedNameToStringArray(node));
    }

    private static Expression<String[]> convertQualifiedNameToStringArray(CommonTree node) {
    	int childCount = node.getChildCount();
		String[] result = new String[childCount];
    	List<CommonTree> childNodes = getChildNodes(node);
    	for (int i = 0; i < childCount; i++)
    		result[i] = childNodes.get(i).getText();
		return new ConstantExpression<String[]>(result);
    }

    private static Expression<Class<?>> convertType(CommonTree node) {
    	Expression<String[]> classNameExpression = convertQualifiedNameToStringArray(childAt(0, node));
    	final String className = ArrayFormat.format(".", classNameExpression.evaluate(null));
		PrimitiveType primitiveType = PrimitiveType.getInstance(className);
		if (primitiveType != null)
			return new ConstantExpression<Class<?>>(primitiveType.getJavaType());
		else {
	    	return new UnaryExpression<Class<?>>(classNameExpression) {
				public Class<?> evaluate(Context context) {
					return DefaultClassProvider.resolveByObjectOrDefaultInstance(className, context);
	            }
	    	};
		}
    }

    private static Expression<? extends Number> convertIntLiteral(CommonTree node) {
		String text = node.getText();
		Number number = null;
		if (text.length() > 10)
			number = Long.parseLong(text);
		else if (text.length() == 10) {
			long l = Long.parseLong(text);
			if (l <= Integer.MAX_VALUE)
				number = (int) l;
			else
				number = l;
		} else
			number = Integer.parseInt(text);
		return new ConstantExpression<Number>(number);
    }

    private static Expression<Double> convertDecimalLiteral(CommonTree node) {
		return new ConstantExpression<Double>(Double.parseDouble(node.getText()));
    }

    private static Expression<?> convertCreator(CommonTree node) throws ParseException {
		List<CommonTree> childNodes = getChildNodes(node);
    	Expression<Class<?>> classExpression = convertQualifiedNameOfClass(childNodes.get(0));
    	Expression<?>[] params = parseArguments(childNodes.get(1));
    	return new Construction(classExpression, params);
    }

    private static Expression<?> convertBean(CommonTree node) throws ParseException {
		List<CommonTree> childNodes = getChildNodes(node);
    	Expression<Class<?>> classExpression = convertQualifiedNameOfClass(childNodes.get(0));
    	Assignment[] props = parseFieldAssignments(childNodes, 1);
    	return new BeanConstruction(classExpression, props);
    }

    private static Assignment[] parseFieldAssignments(List<CommonTree> nodes, int firstIndex) throws ParseException {
    	Assignment[] assignments = new Assignment[nodes.size() - firstIndex];
    	for (int i = firstIndex; i < nodes.size(); i++) {
    		CommonTree assignmentNode = nodes.get(i);
    		CommonTree nameNode = childAt(0, assignmentNode);
    		String name = nameNode.getText();
    		CommonTree exNode = childAt(1, assignmentNode);
    		Expression<?> ex = convertNode(exNode);
			assignments[i - firstIndex] = new Assignment(name, ex);
    	}
		return assignments;
    }

	private static Expression<?> convertInvocation(CommonTree node) throws ParseException {
    	String[] qn = convertQualifiedNameToStringArray(childAt(0, node)).evaluate(null);
    	Expression<?>[] argExpressions = parseArguments(childAt(1, node));
    	return new QNInvocationExpression<Object>(qn, argExpressions);
    }

    private static Expression<?> convertSubInvocation(CommonTree node) throws ParseException {
    	Expression<?> object = convertNode(childAt(0, node));
    	String methodMame = (String) convertNode(childAt(1, node)).evaluate(null);
    	Expression<?>[] argsExpressions = parseArguments(childAt(2, node));
//    	Expression<Expression<?>[]> paramExpression = new ConstantExpression<Expression<?>[]>(params);
    	return new InvocationExpression(object, methodMame, argsExpressions);
    }

    private static Expression<?> convertIndex(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
	            Object container = part1.evaluate(context);
	            Object indexObject = part2.evaluate(context);
	            if (container instanceof List) {
					int index = AnyConverter.convert(indexObject, Integer.class);
	            	return ((List<?>) container).get(index);
	            } else if (container.getClass().isArray()) {
					int index = AnyConverter.convert(indexObject, Integer.class);
	            	return ((Object[]) container)[index];
	            } else if (container instanceof String) {
					int index = AnyConverter.convert(indexObject, Integer.class);
	            	return ((String) container).charAt(index);
	            } else if (container instanceof Map) {
	            	return ((Map<?,?>) container).get(indexObject);
	            } else
	            	throw new IllegalArgumentException("Cannot do index-based access on " 
	            			+ BeanUtil.simpleClassName(container));
            }
		};
    }

    private static Expression<?> convertField(CommonTree node) throws ParseException {
    	return new FieldExpression(convertNode(childAt(0, node)), convertIdentifier(childAt(1, node)).evaluate(null));
    }

    private static Expression<?> convertCast(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
	            Class<?> targetType = (Class<?>) part1.evaluate(context);
	            Object source = part2.evaluate(context);
	            return AnyConverter.convert(source, targetType);
            }
		};
    }

    private static Expression<?> convertNegation(CommonTree node) throws ParseException {
		return new UnaryExpression<Object>(convertNode(childAt(0, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().negate(term.evaluate(context));
            }
		};
    }

    private static Expression<?> convertLogicalComplement(CommonTree node) throws ParseException {
		return new UnaryExpression<Object>(convertNode(childAt(0, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().logicalComplement(term.evaluate(context));
            }
		};
    }

    private static Expression<?> convertBitwiseComplement(CommonTree node) throws ParseException {
		return new UnaryExpression<Object>(convertNode(childAt(0, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().bitwiseComplement(term.evaluate(context));
            }
		};
    }

	private static Expression<Class<?>> convertQualifiedNameOfClass(CommonTree node) {
		List<CommonTree> childNodes = getChildNodes(node);
		StringBuffer className = new StringBuffer();
		for (CommonTree childNode : childNodes) {
			if (className.length() > 0)
				className.append('.');
			className.append(childNode.getText());
		}
		return new ClassExpression(className.toString());
    }

    private static Expression<?>[] parseArguments(CommonTree node) throws ParseException {
		List<CommonTree> childNodes = getChildNodes(node);
		if (childNodes == null)
			return EMPTY_ARGUMENT_LIST;
		Expression<?>[] result = new Expression[childNodes.size()];
		for (int i = 0; i < childNodes.size(); i++) {
			CommonTree childNode = childNodes.get(i);
			result[i] = convertNode(childNode);
		}
		return result;
    }

	private static Expression<?> convertPlus(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
			    Expression<?>[] summands = { part1, part2 };
				Assert.isTrue(summands.length > 1, "At least two summands needed");
                Object result = summands[0].evaluate(context);
                for (int i = 1; i < summands.length; i++)
                	result = ArithmeticEngine.defaultInstance().add(result, summands[i].evaluate(context));
                return result;
		    }
		};
    }

	private static Expression<?> convertMinus(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
			    return ArithmeticEngine.defaultInstance().subtract(part1.evaluate(context), part2.evaluate(context));
		    }
		};
    }

	private static Expression<?> convertStar(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
			    return ArithmeticEngine.defaultInstance().multiply(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<?> convertSlash(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
			    return ArithmeticEngine.defaultInstance().divide(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertPercent(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().mod(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertShiftLeft(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().shiftLeft(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertShiftRight(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().shiftRight(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertShiftRight2(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().shiftRightUnsigned(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertAnd(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().bitwiseAnd(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertInclusiveOr(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().bitwiseOr(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Object> convertExclusiveOr(CommonTree node) throws ParseException {
		return new BinaryExpression<Object>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Object evaluate(Context context) {
				return ArithmeticEngine.defaultInstance().bitwiseExclusiveOr(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertEquals(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                return ArithmeticEngine.defaultInstance().equals(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertNotEquals(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                return !ArithmeticEngine.defaultInstance().equals(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertLess(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                return ArithmeticEngine.defaultInstance().less(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertLessOrEquals(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                return ArithmeticEngine.defaultInstance().lessOrEquals(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertGreater(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                return ArithmeticEngine.defaultInstance().greater(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertGreaterOrEquals(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                ArithmeticEngine engine = ArithmeticEngine.defaultInstance();
				return engine.greaterOrEquals(part1.evaluate(context), part2.evaluate(context));
            }
		};
    }

    private static Expression<Boolean> convertConditionalOr(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                boolean b1 = AnyConverter.convert(part1.evaluate(context), Boolean.class);
                boolean b2 = AnyConverter.convert(part2.evaluate(context), Boolean.class);
				return b1 || b2;
            }
		};
    }

    private static Expression<Boolean> convertConditionalAnd(CommonTree node) throws ParseException {
		return new BinaryExpression<Boolean>(convertNode(childAt(0, node)), convertNode(childAt(1, node))) {
			public Boolean evaluate(Context context) {
                boolean b1 = AnyConverter.convert(part1.evaluate(context), Boolean.class);
                boolean b2 = AnyConverter.convert(part2.evaluate(context), Boolean.class);
				return b1 && b2;
            }
		};
    }

    private static Expression<?> convertConditionalExpression(CommonTree node) throws ParseException {
		return new ConditionalExpression(
				convertNode(childAt(0, node)),  // condition
				convertNode(childAt(1, node)),  // true alternative 
				convertNode(childAt(2, node))); // false alternative
    }
    
    // CommonTree helpers ----------------------------------------------------------------------------------------------

    private static CommonTree childAt(int index, CommonTree node) {
	    return (CommonTree) node.getChild(index);
    }

	@SuppressWarnings("unchecked")
    private static List<CommonTree> getChildNodes(CommonTree node) {
	    return node.getChildren();
    }

}
