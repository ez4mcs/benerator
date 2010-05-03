/*
 * (c) Copyright 2010 by Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License (GPL).
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

package org.databene.benerator.anno;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.databene.benerator.distribution.sequence.StepSequence;
import org.databene.model.data.ArrayElementDescriptor;
import org.databene.model.data.ArrayTypeDescriptor;
import org.databene.model.data.SimpleTypeDescriptor;
import org.junit.Test;

/**
 * Tests the {@link AnnotationMapper}.<br/><br/>
 * Created: 30.04.2010 13:57:59
 * @since 0.6.1
 * @author Volker Bergmann
 */
public class AnnotationMapperTest {

	@Test
	public void testUnannotated() throws Exception {
		checkMethod("unannotatedMethod", String.class, "string", "pattern", null);
	}

	public void unannotatedMethod(String name) { }

	
	
	@Test
	public void testGenerator() throws Exception {
		checkMethod("generatorMethod", String.class, "string", "generator", "myGen");
	}

	public void generatorMethod(@Generator("myGen") String name) { }

	
	
	@Test
	public void testNullQuota() throws Exception {
		checkMethod("nullQuotaMethod", String.class, "string", "nullQuota", 1.);
	}

	public void nullQuotaMethod(@NullQuota(1) String name) { }

	
	
	@Test
	public void testUnique() throws Exception {
		checkMethod("uniqueMethod", String.class, "string", "unique", true);
	}

	public void uniqueMethod(@Unique String name) { }

	
	
	@Test
	public void testValues() throws Exception {
		checkMethod("valuesMethod", String.class, "string", "values", "A, B" );
	}

	public void valuesMethod(@Values({"A", "B"}) String name) { }

	
	
	@Test
	public void testPattern() throws Exception {
		checkMethod("patternMethod", String.class, "string", "pattern", "ABC");
	}

	public void patternMethod(@Pattern("ABC") String name) { }

	
	
	@Test
	public void testPatternMinMaxLength() throws Exception {
		checkMethod("patternMinMaxLengthMethod", String.class, "string", "pattern", "[A-Z]*", "minLength", 5, "maxLength", 8);
	}

	public void patternMinMaxLengthMethod(@Pattern("[A-Z]*") @MinLength(5) @MaxLength(8) String name) { }
	

	
	@Test
	public void testDbSource() throws Exception {
		checkMethod("dbSourceMethod", String.class, "string", 
				"source", "db", 
				"selector", "select id from db_user");
	}

	public void dbSourceMethod(@Source(id = "db", selector ="select id from db_user") String name) { }
	

	
	@Test
	public void testFileSource() throws Exception {
		checkMethod("fileSourceMethod", String.class, "string", 
				"source", "customers.csv", 
				"dataset", "DE",
				"nesting", "region",
				"separator", ";", 
				"encoding", "UTF-8", 
				"filter", "candidate.age >= 18");
	}

	public void fileSourceMethod(
			@Source(uri = "customers.csv", dataset = "DE", nesting = "region", separator = ";", 
					encoding = "UTF-8", filter ="candidate.age >= 18")
			String name) { 
	}
	

	
	// test number generation settings ---------------------------------------------------------------------------------
	
	@Test
	public void testStdSequenceInt() throws Exception {
		checkMethod("predefSequenceIntMethod", int.class, "int", 
				"min", "3.0", 
				"max", "8.0", 
				"precision", "2.0",
				"distribution", "cumulated");
	}

	public void predefSequenceIntMethod(@Min(3) @Max(8) @Precision(2) @Distribution("cumulated") int n) { }
	

	
	@Test
	public void testSequenceClassInt() throws Exception {
		checkMethod("sequenceClassIntMethod", int.class, "int", 
				"distribution", StepSequence.class.getName());
	}

	public void sequenceClassIntMethod(
			@Distribution("org.databene.benerator.distribution.sequence.StepSequence") int n) { }
	

	
	@Test
	public void testSequenceCtorInt() throws Exception {
		checkMethod("sequenceCtorIntMethod", int.class, "int", 
				"distribution", "new " + StepSequence.class.getName() + "()");
	}

	public void sequenceCtorIntMethod(
			@Distribution("new org.databene.benerator.distribution.sequence.StepSequence()") int n) { }
	
	
	
	// testing lengths -------------------------------------------------------------------------------------------------
	
	@Test
	public void testPredefLengthSequenceInt() throws Exception {
		checkMethod("predefLengthSequenceIntMethod", String.class, "string", 
				"minLength", 3, 
				"maxLength", 8, 
				"lengthDistribution", "cumulated");
	}

	public void predefLengthSequenceIntMethod(
			@MinLength(3) @MaxLength(8) @LengthDistribution("cumulated") String s) { }
	

	
	@Test
	public void testLengthSequenceClassInt() throws Exception {
		checkMethod("lengthSequenceClassIntMethod", String.class, "string", 
				"lengthDistribution", StepSequence.class.getName());
	}

	public void lengthSequenceClassIntMethod(
			@LengthDistribution("org.databene.benerator.distribution.sequence.StepSequence") String s) { }
	

	
	@Test
	public void testLengthSequenceCtorInt() throws Exception {
		checkMethod("lengthSequenceCtorIntMethod", String.class, "string", 
				"lengthDistribution", "new " + StepSequence.class.getName() + "()");
	}

	public void lengthSequenceCtorIntMethod(
			@LengthDistribution("new org.databene.benerator.distribution.sequence.StepSequence()") String s) { }
	
	
	
	// helper method ---------------------------------------------------------------------------------------------------
	
	private void checkMethod(String method, Class<?> methodArgType, String expectedType, Object ... details)
            throws NoSuchMethodException {
	    Method stringMethod = getClass().getDeclaredMethod(method, new Class[] { methodArgType });
		ArrayTypeDescriptor arrayDescriptor = AnnotationMapper.mapMethodParams(stringMethod);
		assertEquals(1, arrayDescriptor.getElements().size());
		ArrayElementDescriptor param1 = arrayDescriptor.getElement(0);
		assertEquals(expectedType, ((SimpleTypeDescriptor) param1.getTypeDescriptor()).getPrimitiveType().getName());
		for (int i = 0; i < details.length; i += 2) {
	        String detailName = (String) details[i];
	        Object expectedValue = details[i + 1];
			Object actualValue;
			if (param1.supportsDetail(detailName))
				actualValue = param1.getDetailValue(detailName);
			else
				actualValue = param1.getTypeDescriptor().getDetailValue(detailName);
			assertEquals(expectedValue, actualValue);
        }
    }
	
}
