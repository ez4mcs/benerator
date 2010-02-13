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

package org.databene.benerator.distribution.sequence;

import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.benerator.primitive.number.AbstractNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates 'Double' values for the 'wedge' sequence.<br/><br/>
 * Created: 13.02.2010 13:06:27
 * @since 0.6.0
 * @author Volker Bergmann
 */
public class WedgeDoubleGenerator extends AbstractNumberGenerator<Double> {

    private static final Logger logger = LoggerFactory.getLogger(WedgeDoubleGenerator.class);

    private Double cursor;
    private double end;

    public WedgeDoubleGenerator() {
        this(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public WedgeDoubleGenerator(double min, double max) {
        this(min, max, 1);
    }

    public WedgeDoubleGenerator(double min, double max, double precision) {
        super(Double.class, min, max, precision);
        this.cursor = min;
    }

    // generator interface ---------------------------------------------------------------------------------------------

    @Override
	public void validate() {
        if (dirty) {
            cursor = min;
            max = min + (max - min) / precision * precision;
            super.validate();
            double steps = (max - min) / precision + 1;
            end = min + Math.floor(steps / 2) * precision;
            this.dirty = false;
            if (logger.isDebugEnabled())
                logger.debug("validated state: " + this);
        }
    }

    public Double generate() throws IllegalGeneratorStateException {
        if (dirty)
            validate();
        if (cursor == null)
            return null;
        double result = cursor;
        if (Math.abs(cursor - end) < precision / 2)
            cursor = null;
        else {
            cursor = max - cursor + min;
            if (cursor < end)
                cursor += precision;
        }
        return result;
    }

    @Override
	public void reset() {
        super.reset();
        this.cursor = min;
    }

    @Override
	public void close() {
        super.close();
        this.cursor = null;
    }
    
}
