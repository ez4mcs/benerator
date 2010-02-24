/*
 * (c) Copyright 2007-2010 by Volker Bergmann. All rights reserved.
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

package org.databene.benerator.sample;

import org.databene.benerator.util.AbstractGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a predefined sequence of objects.<br/>
 * <br/>
 * Created: 19.11.2007 15:21:24
 */
public class SequenceGenerator<E> extends AbstractGenerator<E> {
    
    private static Logger logger = LoggerFactory.getLogger(SequenceGenerator.class);

    private Class<E> productType;
    private E[] values;
    private int cursor = 0;

    public SequenceGenerator(Class<E> productType, E ... values) {
        this.productType = productType;
        this.values = values;
    }

    // Generator interface ---------------------------------------------------------------------------------------------

    public Class<E> getGeneratedType() {
        return productType;
    }

    public E generate() {
        if (cursor < 0)
            return null;
        E result = values[cursor];
        if (cursor < values.length - 1)
            cursor++;
        else
            cursor = -1;
        if (logger.isDebugEnabled())
            logger.debug("created: " + result);
        return result;
    }

    public void reset() {
        cursor = 0;
    }

    public void close() {
        values = null;
        cursor = -1;
    }
    
}
