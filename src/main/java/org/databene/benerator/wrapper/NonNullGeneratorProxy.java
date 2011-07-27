/*
 * (c) Copyright 2011 by Volker Bergmann. All rights reserved.
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

package org.databene.benerator.wrapper;

import org.databene.benerator.NonNullGenerator;

/**
 * TODO Document class.<br/><br/>
 * Created: 27.07.2011 11:57:17
 * @since 0.7.0
 * @author Volker Bergmann
 */
public abstract class NonNullGeneratorProxy<E> extends GeneratorProxy<E> implements NonNullGenerator<E> {
	
    // constructors ----------------------------------------------------------------------------------------------------

    public NonNullGeneratorProxy() {
        this(null);
    }
    
    public NonNullGeneratorProxy(NonNullGenerator<E> source) {
        super(source);
    }
    
    /** Returns the source generator */
    @Override
	public NonNullGenerator<E> getSource() {
        return (NonNullGenerator<E>) super.getSource();
    }

    /** Sets the source generator */
    public void setSource(NonNullGenerator<E> source) {
        super.setSource(source);
    }
    
    protected final E generateFromNotNullSource() {
		return getSource().generate();
    }

	@Override
	public final ProductWrapper<E> generate(ProductWrapper<E> wrapper) {
		E result = generate();
		return (result != null ? wrapper.wrap(result) : null);
	}

	public E generate() {
		return generateFromNotNullSource();
	}

}
