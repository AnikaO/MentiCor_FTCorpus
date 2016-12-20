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
 * Reformats the output of TransformDrugBankIntoBratDB.groovy to achieve format necessary for upload to annotation server. The format of the output of the 
 * initial script is first a line with identifier and name of the drug and in consecutive lines, synonyms that are intended by tab. 
 * </p>
 * 
 * @author Anika Oellrich
 * 
 * @param file	Output file from TransformDrugBankIntoBratDB.groovy script
 */
def id = ""
def name = ""
def synonyms = []

// go through all the lines
new File(args[0]).splitEachLine("\t") { splits ->
	if (splits.size() > 0) {

		// record synonyms
		if (splits.size() == 1) {
			if (!splits[0].trim().equals("")) {
				synonyms.add(splits[0].trim())
			}
			
		// record concept identifier and name
		} else if (splits.size() == 2) {
		
			// print information about previous drug 
			if (!id.equals("")) {
				print id + "\tname:Name:" + name g
				
				if (synonyms.size() > 0) {
					synonyms.each { syn ->
						print "\tname:Synonym:" + syn
					}
				}
				
				print "\n"
			}
			
			// reset data to collect for new drug
			name = splits[1]
			id = splits[0]
			synonyms.clear()
		}
	}
}
