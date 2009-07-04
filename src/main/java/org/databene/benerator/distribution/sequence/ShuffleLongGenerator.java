/*
 * (c) Copyright 2006-2009 by Volker Bergmann. All rights reserved.
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

package org.databene.benerator.distribution.sequence;

import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.benerator.primitive.number.AbstractNumberGenerator;

/**
 * Long Generator that implements a 'shuffle' Long Sequence.<br/>
 * <br/>
 * Created: 18.06.2006 14:40:29
 * @author Volker Bergmann
 */
public class ShuffleLongGenerator extends AbstractNumberGenerator<Long> {

    private long cursor;
    private long increment;

    public ShuffleLongGenerator() {
        this(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public ShuffleLongGenerator(long min, long max) {
        this(min, max, 1);
    }

    public ShuffleLongGenerator(long min, long max, long increment) {
        super(Long.class, min, max, increment);
        this.increment = increment;
        reset();
    }

    // config properties -----------------------------------------------------------------------------------------------

    public long getIncrement() {
        return increment;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
        this.dirty = true;
    }

    // Generator interface ---------------------------------------------------------------------------------------------

    @Override
	public void validate() {
        if (dirty) {
            cursor = min;
            super.validate();
            this.dirty = false;
        }
    }

    public Long generate() throws IllegalGeneratorStateException {
        if (dirty)
            validate();
        long result = cursor;
        long increment = getIncrement();
        if (cursor + increment <= max)
            cursor += increment;
        else
            cursor = min + ((cursor - min + 1) % increment);
        return result;
    }
    
    @Override
    public void reset() {
    	this.cursor = min;
    }

}
