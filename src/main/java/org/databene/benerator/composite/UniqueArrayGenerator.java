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

package org.databene.benerator.composite;

import org.databene.benerator.GeneratorContext;
import org.databene.benerator.nullable.NullableGenerator;
import org.databene.benerator.util.GeneratorUtil;
import org.databene.benerator.wrapper.CompositeGenerator;
import org.databene.benerator.wrapper.ProductWrapper;
import org.databene.benerator.wrapper.ThreadLocalProductWrapper;

/**
 * Uses one dedicated generator for each array element and combines their outputs in a cartesian product.<br/><br/>
 * Created: 16.05.2010 10:05:12
 * @since 0.6.2
 * @author Volker Bergmann
 */
public class UniqueArrayGenerator extends CompositeGenerator<Object[]> {
	// TODO v0.7 check and clean up all *ArrayGenerator classes
	
	private ThreadLocalProductWrapper<?> threadLocalWrapper = new ThreadLocalProductWrapper<Object>();
	private NullableGenerator<?>[] sources;
	private int arrayLength;
	private Object[] buffer;
	
	public UniqueArrayGenerator(NullableGenerator<?>[] sources) {
		super(Object[].class);
		this.sources = sources;
		this.arrayLength = sources.length;
		registerComponents(sources);
	}
	
	@Override
	public synchronized void init(GeneratorContext context) {
	    GeneratorUtil.initAll(sources, context);
	    initBuffer();
	    super.init(context);
	}

	@Override
	public void reset() {
		GeneratorUtil.resetAll(sources);
	    initBuffer();
	    super.reset();
	}
	
	public Object[] generate() {
		assertInitialized();
		fetchNext(arrayLength - 1);
	    return buffer;
    }

	private void initBuffer() {
	    buffer = new Object[sources.length];
	    for (int i = 0; i < sources.length - 1; i++) { // init all elements except the last one
	        ProductWrapper<?> wrapper = generateAt(i);
	        if (wrapper == null) {
	        	buffer = null;
	        	break;
	        } else
	        	buffer[i] = wrapper.product;
        }
    }

	private void fetchNext(int index) {
		if (buffer == null)
			return;
		ProductWrapper<?> wrapper = generateAt(index);
		if (wrapper != null) {
			buffer[index] = wrapper.product;
		} else if (index > 0) {
			sources[index].reset();
			fetchNext(index);
			fetchNext(index - 1);
		} else
			buffer = null;
    }

	@SuppressWarnings("unchecked")
    private ProductWrapper<?> generateAt(int index) {
	    @SuppressWarnings("rawtypes")
		ProductWrapper wrapper = threadLocalWrapper.get();
	    wrapper = sources[index].generate(wrapper);
	    return wrapper;
    }

}
