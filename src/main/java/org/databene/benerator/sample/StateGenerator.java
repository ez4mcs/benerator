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

import java.util.HashMap;
import java.util.Map;

import org.databene.benerator.Generator;
import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.benerator.InvalidGeneratorSetupException;
import org.databene.commons.StringUtil;

/**
 * Generates states as configured by a state machine.<br/>
 * <br/>
 * Created at 17.07.2009 05:41:47
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class StateGenerator<E> implements Generator<E> {
	
	private static final int UNINITIALIZED = 0;
	private static final int INITIALIZED = 1;
	private static final int CLOSED = 2;
	
	private Class<E> stateType;
	private E nextState;
	private Map<E, MappedSampleGenerator<E>> transitions;
	private int phase = UNINITIALIZED;

    @SuppressWarnings("unchecked")
    public StateGenerator(String transitionSpec) { // TODO move String functionality to class StringStateGenerator?
	    this((Class<E>) String.class);
	    setTransitions(transitionSpec);
    }
    
    public StateGenerator(Class<E> stateType) {
	    this.stateType = stateType;
	    this.transitions = new HashMap<E, MappedSampleGenerator<E>>();
	    this.nextState = null;
    }
    
    @SuppressWarnings("unchecked")
    public void setTransitions(String transitionSpec) { // TODO move String functionality to class StringStateGenerator?
    	String[] tokens = transitionSpec.split(",");
    	for (String token : tokens) { // TODO use ANTLR for parsing
    		int minusIndex = token.indexOf("->");
    		String from = StringUtil.emptyToNull(token.substring(0, minusIndex));
    		int toIndex = minusIndex + 2;
    		int probIndex = token.indexOf('[');
    		String to;
    		double weight;
    		if (probIndex >= 0) {
    			to = StringUtil.emptyToNull(token.substring(toIndex, probIndex));
    			weight = Double.parseDouble(token.substring(probIndex + 1, token.indexOf(']')));
    		} else {
    			to = StringUtil.emptyToNull(token.substring(toIndex));
    			weight = 1.;
    		}
    		addTransition((E) from, (E) to, weight);
    	}
    }

    public void addTransition(E from, E to, double weight) {
    	MappedSampleGenerator<E> subGenerator = transitions.get(from);
    	if (subGenerator == null) {
    		subGenerator = new MappedSampleGenerator<E>(stateType);
    		transitions.put(from, subGenerator);
    	}
    	subGenerator.addSample(to, weight);
    }

    public Class<E> getGeneratedType() {
	    return stateType;
    }
    
    public void validate() throws InvalidGeneratorSetupException {
    	if (phase == UNINITIALIZED) {
	        MappedSampleGenerator<E> gen = this.transitions.get(null);
	        if (gen == null)
	        	throw new InvalidGeneratorSetupException("No initial state defined for " + this);
	        boolean hasEndTransition = false;
	        for (MappedSampleGenerator<E> tmp : transitions.values())
	        	if (tmp.containsSample(null)) {
	        		hasEndTransition = true;
	        		break;
	        	}
	        if (!hasEndTransition)
	        	throw new InvalidGeneratorSetupException("No final state defined for " + this);
	        nextState = gen.generate();
	        phase = INITIALIZED;
        }
    }
	
    public boolean available() {
    	if (phase == UNINITIALIZED)
    		validate();
    	return (phase == INITIALIZED && nextState != null);
    }
    
    public E generate() throws IllegalGeneratorStateException {
    	if (phase == CLOSED)
    		throw new IllegalGeneratorStateException("Generator not available: " + this);
    	if (phase == UNINITIALIZED)
    		validate();
    	E result = nextState;
	    MappedSampleGenerator<E> subGenerator = transitions.get(nextState);
		nextState = subGenerator.generate();
		if (nextState == null)
			phase = CLOSED;
		return result;
    }

    public void reset() throws IllegalGeneratorStateException {
	    phase = UNINITIALIZED;
    }
    
    public void close() {
    	phase = CLOSED;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + transitions;
    }

}
