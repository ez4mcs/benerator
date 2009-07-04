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

package org.databene.benerator.distribution.sequence;

import java.util.Random;

import org.databene.benerator.InvalidGeneratorSetupException;
import org.databene.benerator.PropertyMessage;
import org.databene.benerator.primitive.number.adapter.AbstractNumberGenerator;

/**
 * Creates random {@link Integer} values with a uniform distribution.<br/>
 * <br/>
 * Created at 24.06.2009 00:57:52
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class RandomIntegerGenerator extends AbstractNumberGenerator<Integer> {

    private static final int DEFAULT_MIN = Integer.MIN_VALUE / 2 + 1; // test if it works with these min/max values
	private static final int DEFAULT_MAX = Integer.MAX_VALUE / 2 - 1;
	private static final int DEFAULT_PRECISION = 1;

	private static Random random = new Random();

    // constructors ----------------------------------------------------------------------------------------------------

    public RandomIntegerGenerator() {
    	this(DEFAULT_MIN, DEFAULT_MAX);
    }

    public RandomIntegerGenerator(int min, int max) {
        this(min, max, DEFAULT_PRECISION);
    }

    public RandomIntegerGenerator(int min, int max, int precision) {
        super(Integer.class, min, max, precision);
    }

    // Generator implementation ----------------------------------------------------------------------------------------

    public Integer generate() {
        return generate(min, max, precision);
    }
    
    // public convenience method ---------------------------------------------------------------------------------------

    public static int generate(int min, int max, int precision) {
        if (min > max)
            throw new InvalidGeneratorSetupException(new PropertyMessage("min", "greater than max"));
        int range = (max - min + precision) / precision;
        int result;
        if (range != 0)
            result = min + Math.abs(random.nextInt() % range) * precision;
        else
            result = random.nextInt() * precision;
        if (result < min)
            result += range;
        return result;
    }

}
