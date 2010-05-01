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

package org.databene.benerator.engine.parser.xml;

import org.databene.benerator.engine.BeneratorContext;
import org.databene.benerator.engine.Statement;
import org.databene.benerator.engine.statement.ImportStatement;
import org.databene.commons.ConfigurationError;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link ImportParser} and {@link ImportStatement}.<br/><br/>
 * Created: 01.05.2010 07:24:25
 * @since 0.6.1
 * @author Volker Bergmann
 */
public class ImportParserAndStatementTest extends ParserTest {

	@Override
    @Before
	public void setUp() {
		super.setUp();
		parser = new ImportParser();
	}
	
	@Test(expected = ConfigurationError.class)
	public void testNoImport() {
		context.forName("IncrementGenerator");
	}
	
	@Test
	public void testDefaults() {
		Statement statement = parse("<import defaults='true' />");
		BeneratorContext context = new BeneratorContext();
		statement.execute(context);
		context.forName("IncrementGenerator");
	}
	
	@Test
	public void testPlatforms() {
		Statement statement = parse("<import platforms='db, xml' />");
		BeneratorContext context = new BeneratorContext();
		statement.execute(context);
		context.forName("DBSystem");
		context.forName("XMLEntityExporter");
	}
	
	@Test
	public void testDomains() {
		Statement statement = parse("<import domains='person, address' />");
		BeneratorContext context = new BeneratorContext();
		statement.execute(context);
		context.forName("PersonGenerator");
		context.forName("AddressGenerator");
	}
	
	@Test(expected = ConfigurationError.class)
	public void testImportAttributeTypo() throws Exception {
		parse("<import platfrmo='typo' />");
	}

}
