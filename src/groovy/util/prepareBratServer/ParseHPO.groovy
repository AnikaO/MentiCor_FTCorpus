/*
 * Copyright (c) <2016>, <Anika Oellrich>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
 *    in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
 *    derived from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package util.prepareBratServer

/**
 * <p>
 * Script to extract identifiers and concept labels from the Human Phenotype Ontology (HPO) obo/owl file and generate a file out of it that can be loaded into
 * Brat for normalisation purposes. The details for activating normalisation in Brat are further described <a href="http://brat.nlplab.org/normalization.html">
 * online</a>. This script expects one parameter which is the path the ontology file. Results are printed to the executing console instead of a file.
 * </p><p>
 * Relies on <a href="https://github.com/loopasam/Brain">Brain library</a> for an easy handling of ontology files. 
 * </p>
 * 
 * @author Anika Oellrich
 * 
 * @param	file	Path to HPO ontology file
 */
import uk.ac.ebi.brain.core.Brain

// initialise Brain object
Brain brain = new Brain();

// read the ontology file
brain.learn(args[0])

// retrieve all defined concepts as a list
List<String> subClasses = brain.getSubClassesFromLabel("All", false);

// go through each concept ...
subClasses.each { sCl ->
	// ... print identifier and name ...
	print sCl.replace("_", ":") + "\tname:Name:" + brain.getLabel(sCl) 
	
	// ... and all the defined EXACT synonyms 
	brain.getOWLClass(sCl).getAnnotations(brain.getOntology()).each { anno ->
		if (anno.getProperty().toString().equals("<http://www.geneontology.org/formats/oboInOWL#hasExactSynonym>")) {
			print "\tname:Synonym:" + anno.getValue().toString().replace("\"","").replace("^^xsd:string","")
		}
	}
	
	print "\n"
}

brain.sleep()
