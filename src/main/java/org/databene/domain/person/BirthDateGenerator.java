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

package org.databene.domain.person;

import java.util.Date;
import java.util.Calendar;

import org.databene.benerator.primitive.datetime.DateGenerator;
import org.databene.benerator.primitive.datetime.LightweightDateGenerator;
import org.databene.benerator.IllegalGeneratorStateException;
import org.databene.commons.TimeUtil;
import org.databene.commons.Period;
import org.databene.model.function.Sequence;

/**
 * TODO documentation<br/><br/>
 * Created: 13.06.2006 07:15:03
 */
public class BirthDateGenerator extends LightweightDateGenerator {

    private int minAgeYears;
    private int maxAgeYears;

    private DateGenerator dateGenerator;

    public BirthDateGenerator() {
        this(18, 80);
    }

    public BirthDateGenerator(int minAgeYears, int maxAgeYears) {
        this.minAgeYears = minAgeYears;
        this.maxAgeYears = maxAgeYears;
        Date today = TimeUtil.today().getTime();
        Calendar min = TimeUtil.calendar(today);
        min.add(Calendar.YEAR, -maxAgeYears);
        Calendar max = TimeUtil.calendar(today);
        max.add(Calendar.YEAR, -minAgeYears);
        dateGenerator = new DateGenerator(min.getTime(), max.getTime(), Period.DAY.getMillis());
        dateGenerator.setDistribution(Sequence.RANDOM);
    }

    public Date generate() throws IllegalGeneratorStateException {
       return dateGenerator.generate();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[minAgeYears=" + minAgeYears + ", maxAgeYears=" + maxAgeYears + ']';
    }
}
