/*
 * (c) Copyright 2008-2009 by Volker Bergmann. All rights reserved.
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

package org.databene.domain.organization;

import org.databene.benerator.Generator;
import org.databene.benerator.csv.WeightedDatasetCSVGenerator;
import org.databene.benerator.primitive.LightweightStringGenerator;
import org.databene.benerator.primitive.regex.RegexStringGenerator;
import org.databene.benerator.sample.ConstantGenerator;
import org.databene.benerator.sample.SequencedCSVSampleGenerator;
import org.databene.benerator.wrapper.AlternativeGenerator;
import org.databene.benerator.wrapper.ConvertingGenerator;
import org.databene.benerator.wrapper.MessageGenerator;
import org.databene.benerator.wrapper.NullableGenerator;
import org.databene.commons.Encodings;
import org.databene.commons.bean.PropertyAccessConverter;
import org.databene.domain.address.City;
import org.databene.domain.address.CityGenerator;
import org.databene.domain.address.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates company names.<br/><br/>
 * Created: 14.03.2008 08:26:44
 * @since 0.5.0
 * @author Volker Bergmann
 */
public class CompanyNameGenerator extends LightweightStringGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(CompanyNameGenerator.class);

    private static final String ORG = "org/databene/domain/organization/";
    private static final String PERS = "org/databene/domain/person/";
    private static final String REGION  = "org/databene/dataset/region";
    
    private String datasetName;
    private boolean sector;
    private boolean location;
    private boolean legalForm;
    
    private AlternativeGenerator<String> core;
    private Generator<String> sectorGenerator;
    private Generator<String> legalFormGenerator;
    private Generator<String> locationGenerator;
    
    public CompanyNameGenerator() {
    	this(true, true, true);
    }
    
    public CompanyNameGenerator(boolean sector, boolean location, boolean legalForm) {
    	this(sector, location, legalForm, Country.getDefault().getIsoCode());
    }

    public CompanyNameGenerator(String dataset) {
    	this(true, true, true, dataset);
    }

    public CompanyNameGenerator(boolean sector, boolean location, boolean legalForm, String datasetName) {
    	this.sector = sector;
    	this.location = location;
    	this.legalForm = legalForm;
        this.datasetName = datasetName;
        setDataset(datasetName);
    }
    
    public void setDataset(String datasetName) {
        initLocationGenerator(datasetName);
        initLegalFormGenerator(datasetName);
        initSectorGenerator(datasetName);
        
        core = new AlternativeGenerator<String>(String.class);
        core.addSource(new RegexStringGenerator("[A-Z]{3}"));
        
        createPersonNameGenerator(datasetName, core);
        createArtificialNameGenerator(core);
        createTechNameGenerator(core);
	}

	public String generate() {
        StringBuilder builder = new StringBuilder(core.generate());
        if (sectorGenerator != null) {
	        String sec = sectorGenerator.generate();
	        if (sec != null)
	            builder.append(' ').append(sec);
        }
        if (locationGenerator != null) {
	        String loc = locationGenerator.generate();
	        if (loc != null)
	            builder.append(' ').append(loc);
        }
        if (legalFormGenerator != null) {
        	builder.append(' ').append(legalFormGenerator.generate());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + datasetName + ']';
    }

    // private helpers -------------------------------------------------------------------------------------------------
    
	private static void createTechNameGenerator(AlternativeGenerator<String> coreGenerator) {
	    try {
            Generator<String> tech = new MessageGenerator("{0}{1}", 
                    new SequencedCSVSampleGenerator<String>(ORG + "tech1.csv"),
                    new SequencedCSVSampleGenerator<String>(ORG + "tech2.csv")
                );
            tech.validate();
	        coreGenerator.addSource(tech);
        } catch (Exception e) {
        	logger.info("Cannot create technical company name generator: " + e.getMessage());
        }
    }

	private static void createArtificialNameGenerator(AlternativeGenerator<String> coreGenerator) {
	    try {
            Generator<String> artificial = new MessageGenerator("{0}{1}", 
                    new SequencedCSVSampleGenerator<String>(ORG + "artificial1.csv"),
                    new SequencedCSVSampleGenerator<String>(ORG + "artificial2.csv")
                );
            artificial.validate();
	        coreGenerator.addSource(artificial);
        } catch (Exception e) {
        	logger.info("Cannot create artificial company name generator: " + e.getMessage());
        }
    }

	private static void createPersonNameGenerator(String datasetName, AlternativeGenerator<String> coreGenerator) {
	    try {
	        Generator<String> person = new MessageGenerator("{0} {1}", 
	                new WeightedDatasetCSVGenerator<String>(PERS + "givenName_male_{0}.csv", datasetName, REGION),
	                new WeightedDatasetCSVGenerator<String>(PERS + "familyName_{0}.csv", datasetName, REGION)
	            );
	        person.validate();
	        coreGenerator.addSource(person);
        } catch (Exception e) {
        	logger.info("Cannot create person-based company name generator: " + e.getMessage());
        }
    }

	private void initSectorGenerator(String datasetName) {
	    if (sector) {
        	try {
        		sectorGenerator = new NullableGenerator<String>(new WeightedDatasetCSVGenerator<String>(
        				ORG + "sector_{0}.csv", datasetName, REGION, Encodings.UTF_8), 0.7);
        	} catch (Exception e) {
        		logger.info("Cannot create sector generator: " + e.getMessage());
        	}
        }
    }

	private void initLegalFormGenerator(String datasetName) {
	    if (legalForm) {
        	try {
        		legalFormGenerator = new WeightedDatasetCSVGenerator<String>(ORG + "legalForm_{0}.csv", 
        				datasetName, REGION, Encodings.UTF_8);
        	} catch (Exception e) {
        		logger.info("Cannot create legal form generator: " + e.getMessage());
        	}
        }
    }

	@SuppressWarnings("unchecked")
    private void initLocationGenerator(String datasetName) {
	    Country country = Country.getInstance(datasetName);
        if (location && country != null) {
        	try {
	            Generator<String> city = new ConvertingGenerator<City, String>(
	            		new CityGenerator(country), new PropertyAccessConverter("name"));
	            locationGenerator = new NullableGenerator<String>(
	                    	new AlternativeGenerator<String>(String.class, 
	                    			new ConstantGenerator<String>(country.getLocalName()), 
	                    			city), 
	                    	0.8
	                );
        	} catch (Exception e) {
        		logger.info("Cannot create location generator: " + e.getMessage());
                locationGenerator = new ConstantGenerator<String>(null);
        	}
        } else
            locationGenerator = new ConstantGenerator<String>(null);
    }


}
