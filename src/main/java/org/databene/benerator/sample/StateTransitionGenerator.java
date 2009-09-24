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

package org.databene.benerator.sample;

import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.benerator.wrapper.GeneratorWrapper;

/**
 * Generates state transitions of a state machine.<br/>
 * <br/>
 * Created at 17.07.2009 08:04:12
 * @since 0.6.0
 * @author Volker Bergmann
 */

@SuppressWarnings("unchecked")
public class StateTransitionGenerator<E> extends GeneratorWrapper<E, StateTransition> {
	
	private E currentState;
	
    public StateTransitionGenerator(Class<E> stateType) {
	    super(new StateGenerator<E>(stateType));
	    this.currentState = null;
    }

    public void addTransition(E from, E to, double weight) {
    	((StateGenerator) source).addTransition(from, to, weight);
    }

	public Class<StateTransition> getGeneratedType() {
	    return StateTransition.class;
    }

    public StateTransition generate() throws IllegalGeneratorStateException {
    	E previousState = currentState;
    	currentState = source.generate();
	    return new StateTransition<E>(previousState, currentState);
    }

    @Override
    public void reset() {
    	currentState = null;
    	super.reset();
    }

    @Override
    public void close() {
    	currentState = null;
    	super.close();
    }
    
}
