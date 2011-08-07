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

package org.databene.benerator.composite;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.databene.BeneratorConstants;
import org.databene.benerator.Generator;
import org.databene.benerator.GeneratorContext;
import org.databene.benerator.wrapper.ProductWrapper;
import org.databene.commons.ConfigurationError;
import org.databene.commons.MessageHolder;
import org.databene.commons.Resettable;
import org.databene.commons.ThreadAware;
import org.databene.commons.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offers support for entity or array component generation with or without variable generation.<br/><br/>
 * Created: 13.01.2011 10:52:43
 * @since 0.6.4
 * @author Volker Bergmann
 */
public class ComponentAndVariableSupport<E> implements ThreadAware, MessageHolder, Resettable, Closeable {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentAndVariableSupport.class);
    private static final Logger stateLogger = LoggerFactory.getLogger(BeneratorConstants.STATE_LOGGER);
    
    private String instanceName;
	private Map<String, Generator<?>> variables;
    private List<ComponentBuilder<E>> componentBuilders;
	private GeneratorContext context;
	private String message;
	
	public ComponentAndVariableSupport(String instanceName, Map<String, Generator<?>> variables, 
			List<ComponentBuilder<E>> componentBuilders, GeneratorContext context) {
		this.instanceName = instanceName;
		this.variables = variables;
        this.componentBuilders = (componentBuilders != null ? componentBuilders : new ArrayList<ComponentBuilder<E>>());
		this.context = context;
	}
	
    public void init(GeneratorContext context) {
		initVariables(context);
        initComponents(context);
	}

    private void initVariables(GeneratorContext context) {
        for (Map.Entry<String, Generator<?>> entry : variables.entrySet()) {
        	Generator<?> varGen = entry.getValue();
        	try {
	        	varGen.init(context);
        	} catch (Exception e) {
        		throw new RuntimeException("Error initializing variable '" + entry.getKey() + "': " + varGen, e);
        	}
        }
    }

	protected void initComponents(GeneratorContext context) {
		for (ComponentBuilder<E> compGen : componentBuilders) {
            try {
	            compGen.init(context);
            } catch (RuntimeException e) {
	            throw new ConfigurationError("Error initializing component builder: " + compGen, e);
            }
        }
	}

    public boolean apply(E target) {
		if (!calculateVariables())
			return false;
        if (target != null && !buildComponents(target))
        	return false;
    	LOGGER.debug("Generated {}", target);
    	return true;
	}

    public void reset() {
		for (Generator<?> variable : variables.values())
			variable.reset();
        for (ComponentBuilder<E> compGen : componentBuilders)
            compGen.reset();
	}

    public void close() {
		for (Generator<?> variable : variables.values())
			variable.close();
        for (ComponentBuilder<E> compGen : componentBuilders)
            compGen.close();
        for (String variableName : variables.keySet())
            context.remove(variableName);
	}
	
	public String getMessage() {
		return message;
	}
	
	
	
	// helper methods --------------------------------------------------------------------------------------------------
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean calculateVariables() {
		for (Map.Entry<String, Generator<?>> entry : variables.entrySet()) {
			Generator<?> generator = entry.getValue();
			ProductWrapper<?> productWrapper = generator.generate(new ProductWrapper());
			String varName = entry.getKey();
			if (productWrapper == null) {
				this.message = "Variable no more available: " + varName;
	    		LOGGER.debug(message);
	            return false;
			}
	        context.set(varName, productWrapper.unwrap());
		}
		return true;
	}
	
	private boolean buildComponents(E target) {
	    for (ComponentBuilder<E> componentBuilder : componentBuilders) {
            try {
                if (!componentBuilder.buildComponentFor(target)) {
                	message = "Component generator for '" + instanceName + 
                		"' is not available any more: " + componentBuilder;
                    stateLogger.debug(message);
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failure in generation of '" + instanceName + "', " +
                		"Failed component builder: " + componentBuilder, e);
            }
        }
        return true;
    }
	
	
	// ThreadAware interface implementation ----------------------------------------------------------------------------
	
    public boolean isParallelizable() {
	    return ThreadUtil.allParallelizable(variables.values()) && ThreadUtil.allParallelizable(componentBuilders);
    }

    public boolean isThreadSafe() {
	    return ThreadUtil.allThreadSafe(variables.values()) && ThreadUtil.allThreadSafe(componentBuilders);
    }
    
    
    
	// java.lang.Object overrides --------------------------------------------------------------------------------------
	
	@Override
	public String toString() {
	    return getClass().getSimpleName() + "[" + (variables.size() > 0 ? "variables: " + variables : "") + "]";
	}

}
