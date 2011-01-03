/*
 * (c) Copyright 2009-2010 by Volker Bergmann. All rights reserved.
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
import org.databene.benerator.script.BeneratorScriptParser;
import org.databene.benerator.script.Transition;
import org.databene.benerator.script.WeightedTransition;
import org.databene.benerator.wrapper.GeneratorWrapper;
import org.databene.commons.converter.AnyConverter;

/**
 * Generates state transitions of a state machine.<br/>
 * <br/>
 * Created at 17.07.2009 08:04:12
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class StateTransitionGenerator<E> extends GeneratorWrapper<E, Transition> {
	
	private Class<E> stateType;
	private E currentState;
	private boolean done;
	
    public StateTransitionGenerator(Class<E> stateType) {
	    this(stateType, null);
    }

    public StateTransitionGenerator(Class<E> stateType, String transitionSpec) {
	    super(new StateGenerator<E>(stateType));
	    this.stateType = stateType;
	    if (transitionSpec != null) {
	    	WeightedTransition[] transitions = BeneratorScriptParser.parseTransitionList(transitionSpec);
	    	for (WeightedTransition t : transitions)
	    		addTransition(convert(t.getFrom()), convert(t.getTo()), t.getWeight());
	    }
	    this.currentState = null;
	    this.done = false;
    }

    public void addTransition(E from, E to, double weight) {
    	((StateGenerator<E>) source).addTransition(from, to, weight);
    }
    
    // Generator interface implementation ------------------------------------------------------------------------------

	public Class<Transition> getGeneratedType() {
	    return Transition.class;
    }

    public Transition generate() throws IllegalGeneratorStateException {
    	if (done)
    		return null;
    	E previousState = currentState;
    	currentState = source.generate();
    	if (currentState == null)
    		done = true;
	    return new Transition(previousState, currentState);
    }

    @Override
    public void reset() {
    	currentState = null;
	    this.done = false;
    	super.reset();
    }

    @Override
    public void close() {
    	currentState = null;
	    this.done = true;
    	super.close();
    }
    
    // private helpers -------------------------------------------------------------------------------------------------
    
    private E convert(Object object) {
    	return AnyConverter.convert(object, stateType);
    }
    
}
