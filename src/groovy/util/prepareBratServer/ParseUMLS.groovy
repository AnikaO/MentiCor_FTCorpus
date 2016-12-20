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
 * Script to extract UMLS concept identifiers with all corresponding labels assigned to this concept. While other vocabularies may define a 
 * primary term, UMLS does not distinguish between names and synonyms. Also, the UMLS download version is not sorted according to concepts,
 * and information for one concept may be scattered across different files. This unfortunately makes it necessary to read all the files and 
 * log concept names before being able to print. Thus, script demands a lot of memory and computation time can take up to 10 minutes on a 
 * single processor with 5GB RAM. 
 * </p><p>
 * Used files for annotation work: MRCONSO.RRF.aa and MRCONSO.RRF.ab
 * </p><p>
 * Potential future improvement: conceive a less resource demanding version of collecting UMLS concept data.
 * </p>
 * 
 * @author Anika Oellrich
 * 
 * @param	file	Path to the UMLS file needed for identifier, labels and synonyms of UMLS concepts
 */
def umlsMap = [:]

// go through all the files that potentially contain UMLS concept identifiers relevant for the English language
// in the applied version of UMLS, two were recognised as being relevant
args.each { argument ->
	new File(argument).splitEachLine("\\|") { splits ->
		if (splits[1].equals("ENG")) {
			// add concept identifier and label as name 
			if (!umlsMap.containsKey(splits[0])) { umlsMap[splits[0]] = [] }
			
			// add all following names for concept identifier as synonyms
			if (!umlsMap[splits[0]].contains(splits[14])) { umlsMap[splits[0]].add(splits[14])}
		}
	}
}

// print all the concepts with identifiers, names and synonyms
umlsMap.each {
	if (it.value.size() > 0) {
		print it.key
		print "\tname:Name:" + it.value[0]
		
		if (it.value.size() > 1) {
			it.value[1..-1].each { syn -> print "\tname:Synonym:" + syn }
		}
		print "\n"
	}
}
