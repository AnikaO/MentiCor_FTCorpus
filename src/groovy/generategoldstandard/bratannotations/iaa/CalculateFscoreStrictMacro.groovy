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

 package generategoldstandard.bratannotations.iaa

import static groovy.io.FileType.*

/**
 * Script to determine F-score between two curators based on the assigned entity annotations. For this purpose that annotations of one curator are used as 
 * gold standard first and F-score calculated based on that, then the other way round, and then averaged between both. In the case of a strict F-score all
 * attributes such as type, text span, and identifier have to match.
 * 
 * @author Anika Oellrich (anika.oellrich@kcl.ac.uk)
 * 
 * @param folder1	Folder containing annotations for first curator
 * @param folder2	Folder containing annotations for second curator
 */

def curatorDir1 = new File(args[0])
def curatorDir2 = new File(args[1])
// def curator2 = args[2]
def textFiles = []
def orderingCoreScs = ['Bac', 'Met', 'Exp', 'Obs', 'Mod', 'Res', 'Con', 'Obj', 'Mot', 'Goa', 'Hyp']

// read the names of all the annotation files
curatorDir1.eachFileMatch FILES, ~/.*.ann/, { textFiles << it.name}

// go through all the annotation files and calculate F-score for each one of them
textFiles.each { publAnno ->
	def curator1Entities = []
	def curator1Relations = []
	def curator2Entities = []
	def curator2Relations = []

	// read annotations assigned by first curator
	new File(curatorDir1.absolutePath + "/" + publAnno).splitEachLine("\t") { splits ->
		if (splits[0].startsWith("T")) {
			def exp = new Expando()
			exp.id = splits[0]
			exp.type = splits[1]
			exp.start = new Integer(splits[2].toString())
			exp.end = new Integer(splits[3].toString())
			exp.text = splits[4]
			exp.norm = splits[5]
			exp.normId = splits[6]
			curator1Entities.add(exp)
		} else if (splits[0].startsWith("R")) {
			def exp = new Expando()
			exp.id = splits[0]
			exp.type = splits[1]
			exp.e1start = (splits[2])
			exp.e1end = (splits[3])
			exp.e1type = (splits[4])
			exp.e2start = (splits[5])
			exp.e2end = (splits[6])
			exp.e2type = (splits[7])
			curator1Relations.add(exp)
		} 
	}
	
	// read annotations assigned by second curator
	new File(curatorDir2.absolutePath + "/" + publAnno).splitEachLine("\t") { splits ->
		if (splits[0].startsWith("T")) {
			def exp = new Expando()
			exp.id = splits[0]
			exp.type = splits[1]
			exp.start = new Integer(splits[2].toString())
			exp.end = new Integer(splits[3].toString())
			exp.text = splits[4]
			exp.norm = splits[5]
			exp.normId = splits[6]
			curator2Entities.add(exp)
		} else if (splits[0].startsWith("R")) {
			def exp = new Expando()
			exp.id = splits[0]
			exp.type = splits[1]
			exp.e1start = (splits[2])
			exp.e1end = (splits[3])
			exp.e1type = (splits[4])
			exp.e2start = (splits[5])
			exp.e2end = (splits[6])
			exp.e2type = (splits[7])
			curator2Relations.add(exp) 
		} 
	}

	int matches = 0
	
	// count strict matches, i.e. that text span, type and normalised id match
	// there are some case where a double counting needs to be avoided; these cases are when a text span is a list and cannot be normalised, 
	// but have the same type
	curator1Entities.each { ent ->
		def cur2Annos = curator2Entities.findAll{ ( it.start == ent.start ) && ( it.end == ent.end )}
		
		if (cur2Annos.size() > 0) {
			boolean matched = false
			cur2Annos.each { ent2 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				// if ((ent.type == ent2.type) && (ent.norm == ent2.norm) && (!matched)) { // strict_strict
				if (((ent.type == ent2.type) || (ent.norm == ent2.norm)) && (!matched)) { // strict_relaxed
					matches += 1
					matched = true
				} 
			}
		} 
	}
	
	curator1Relations.each { ent ->
		def cur2Annos = curator2Relations.findAll{ ( it.e1start == ent.e1start ) && ( it.e2end == ent.e2end )}

		if (cur2Annos.size() > 0) {
			boolean matched = false
			
			cur2Annos.each { ent2 ->
				if ((ent.type == ent2.type) && !(matched)) {
					matches += 1
					matched = true
				} 
			}
		} 
	} 
	
	// formula f-score = 2 * agree/ ( 2*agree + disagree)
	
	Double fscore = 2 * matches / new Double( curator1Entities.size() + curator2Entities.size() + curator1Relations.size() + curator2Relations.size())
	
	println publAnno + "\t" + fscore // + "\t" + matches + "\t" + curator1Entities.size() + "\t" +  curator2Entities.size() + "\t" + curator1Relations.size() + "\t" + curator2Relations.size()
}