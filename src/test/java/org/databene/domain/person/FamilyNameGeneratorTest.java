/*
 * (c) Copyright 2006-2010 by Volker Bergmann. All rights reserved.
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

package org.databene.domain.person;

import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.benerator.Generator;
import org.databene.benerator.test.GeneratorClassTest;
import org.databene.measure.count.ObjectCounter;
import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * Tests the {@link FamilyNameGenerator}.<br/><br/>
 * Created: 09.06.2006 22:16:06
 * @since 0.1
 * @author Volker Bergmann
 */
public class FamilyNameGeneratorTest extends GeneratorClassTest {

    public FamilyNameGeneratorTest() {
        super(FamilyNameGenerator.class);
    }

    @Test
    public void test() throws IllegalGeneratorStateException {
        ObjectCounter<String> counter = new ObjectCounter<String>(10);
        Generator<String> generator = new FamilyNameGenerator();
        generator.init(context);
        for (int i = 0; i < 10; i++)
            counter.count(generator.generate());
        assertTrue(counter.objectSet().size() >= 3);
    }
    
}
