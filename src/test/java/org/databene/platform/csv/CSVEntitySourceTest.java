/*
 * (c) Copyright 2007-2009 by Volker Bergmann. All rights reserved.
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

package org.databene.platform.csv;

import org.junit.Test;
import static junit.framework.Assert.*;

import org.databene.benerator.engine.BeneratorContext;
import org.databene.model.data.Entity;
import org.databene.model.data.ComplexTypeDescriptor;

import java.util.Iterator;

/**
 * Tests the {@link CSVEntitySource}.<br/><br/>
 * Created: 26.08.2007 12:45:17
 * @author Volker Bergmann
 */
public class CSVEntitySourceTest {

    private static final String URI = "org/databene/platform/csv/person-bean.csv";

    // test methods ----------------------------------------------------------------------------------------------------

    @Test
    public void testSingleRun() {
    	CSVEntitySource source = new CSVEntitySource(URI, "Person", ',');
    	source.setContext(new BeneratorContext());
        checkIteration(source.iterator());
    }

    @Test
    public void testReset() {
    	CSVEntitySource source = new CSVEntitySource(URI, "Person", ',');
    	source.setContext(new BeneratorContext());
        checkIteration(source.iterator());
        checkIteration(source.iterator());
    }

    // private helpers -------------------------------------------------------------------------------------------------

    private void checkIteration(Iterator<Entity> iterator) {
        ComplexTypeDescriptor descriptor = new ComplexTypeDescriptor("Person");
        assertTrue(iterator.hasNext());
        assertEquals(new Entity(descriptor, "name", "Alice", "age", "23"), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(new Entity(descriptor, "name", "Bob", "age", "34"), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(new Entity(descriptor, "name", "Charly", "age", "45"), iterator.next());
        assertFalse(iterator.hasNext());
    }
    
}
