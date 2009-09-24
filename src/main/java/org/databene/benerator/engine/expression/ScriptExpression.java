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

package org.databene.benerator.engine.expression;

import org.databene.commons.Context;
import org.databene.commons.Converter;
import org.databene.commons.Expression;
import org.databene.commons.StringUtil;
import org.databene.commons.converter.AnyConverter;
import org.databene.script.ScriptUtil;

/**
 * TODO document class ScriptExpression.<br/>
 * <br/>
 * Created at 22.07.2009 07:19:44
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class ScriptExpression<E> implements Expression<E> {
	
	private String script;
	private Converter<Object, E> converter;
	private E defaultValue;

    public ScriptExpression(String script, Class<E> resultType) {
    	this(script, resultType, null);
    }

    public ScriptExpression(String script, Class<E> resultType, E defaultValue) {
    	this.script = script;
    	this.converter = new AnyConverter<Object, E>(resultType);
    	this.defaultValue = defaultValue;
    }

	public E evaluate(Context context) {
		if (StringUtil.isEmpty(script))
			return defaultValue;
		else
			return converter.convert(ScriptUtil.render(script, context));
    }

	@Override
	public String toString() {
		return script;
	}
}
