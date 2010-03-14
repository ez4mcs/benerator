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

import org.databene.benerator.*;
import org.databene.benerator.util.LightweightGenerator;
import org.databene.commons.NumberUtil;

/**
 * Long Generator that implements a 'bitreverse' Long Sequence.<br/>
 * <br/>
 * Created: 13.11.2007 14:39:29
 */
public class BitReverseNaturalNumberGenerator extends LightweightGenerator<Long> {

    private long max;
    private long cursor;
    private Long next;
    private int bitsUsed;
    private long maxCursor;

    public BitReverseNaturalNumberGenerator() {
        this(Long.MAX_VALUE);
    }

    public BitReverseNaturalNumberGenerator(long max) {
        this.max = max;
    }

    // config properties -----------------------------------------------------------------------------------------------

	public Class<Long> getGeneratedType() {
        return Long.class;
    }

    public void setMax(Long max) {
        if (max < 0)
            throw new IllegalArgumentException("No negative min supported, was: " + max);
        this.max = max;
    }

    // generator interface ---------------------------------------------------------------------------------------------

    @Override
	public void init(GeneratorContext context) {
    	assertNotInitialized();
    	reset();
        super.init(context);
    }

    public Long generate() throws IllegalGeneratorStateException {
        assertInitialized();
        if (next == null)
            return null;
        long result = next;
        do {
            cursor++;
            next = cursorReversed();
        } while (next > max && cursor < maxCursor);
        if (cursor >= maxCursor)
            next = null;
        return result;
    }

    @Override
	public void reset() {
        cursor = 0;
        bitsUsed = NumberUtil.bitsUsed(max);
        next = 0L;
        this.maxCursor = 1 << bitsUsed;
        super.reset();
    }

    @Override
	public void close() {
        super.close();
        next = null;
    }

    // java.lang.Object overrides --------------------------------------------------------------------------------------

    @Override
	public String toString() {
        return getClass().getSimpleName() + '[' + renderState() + ']';
    }

    // private helpers -------------------------------------------------------------------------------------------------

    private long cursorReversed() {
        long result = 0;
        for (int i = 0; i <= bitsUsed; i++)
            result |= ((cursor >> i) & 1) << (bitsUsed - i - 1);
        return result;
    }

    private String renderState() {
        return "max=" + max + ", cursor=" + cursor;
    }
}
