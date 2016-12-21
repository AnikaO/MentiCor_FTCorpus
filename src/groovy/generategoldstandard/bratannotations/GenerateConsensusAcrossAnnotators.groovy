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
package generategoldstandard.bratannotations

import static groovy.io.FileType.*

/**
 * <p>
 * Merges entity annotations from three different curators based on curator 1 being the most reliable annotator. Directories for entity 
 * annotation files need to be provided as input parameters to the script. Each folder that is provided as input parameter, is expected
 * to hold Brat annotation files (one for each paper in the corpus). These files contain the annotation and normalisation in one line, by 
 * means this is not a proper Brat format. 
 * 
 * @author Anika Oellrich
 * 
 * @param folder1	Folder containing Brat annotation files for curator 1
 * @param folder2	Folder containing Brat annotation files for curator 2
 * @param folder3	Folder containing Brat annotation files for curator 3
 * @param output	Folder to which output for individual papers will be written
 */

def curatorDir1 = new File(args[0])
def curatorDir2 = new File(args[1])
def curatorDir3 = new File(args[2])
def outfolder = new File(args[3])

def textFiles = []


// read the names of all the annotation files
curatorDir1.eachFileMatch FILES, ~/.*.ann/, { textFiles << it.name}

// go through all the annotation files and calculate F-score for each one of them
textFiles.each { publAnno ->
	def curator1Entities = []
	def curator1Relations = []
	def curator2Entities = []
	def curator2Relations = []
	def curator3Entities = []
	def curator3Relations = []

	BufferedWriter bf = new BufferedWriter(new FileWriter(new File(outfolder.absolutePath + "/" + publAnno)))

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

	// read annotations assigned by third curator
	new File(curatorDir3.absolutePath + "/" + publAnno).splitEachLine("\t") { splits ->
		if (splits[0].startsWith("T")) {
			def exp = new Expando()
			exp.id = splits[0]
			exp.type = splits[1]
			exp.start = new Integer(splits[2].toString())
			exp.end = new Integer(splits[3].toString())
			exp.text = splits[4]
			exp.norm = splits[5]
			exp.normId = splits[6]
			curator3Entities.add(exp)
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
			curator3Relations.add(exp)
		}
	}

	// count strict matches, i.e. that text span, type and normalised id match
	// there are some case where a double counting needs to be avoided; these cases are when a text span is a list and cannot be normalised,
	// but have the same type
	curator1Entities.each { ent ->
		def cur2Annos = curator2Entities.findAll{ ( it.start == ent.start ) && ( it.end == ent.end )}
		def cur3Annos = curator3Entities.findAll{ ( it.start == ent.start ) && ( it.end == ent.end )}
		boolean matched = false
		
		if ((cur2Annos.size() > 0) && (cur3Annos.size() > 0)) {
			cur2Annos.each { ent2 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent2.type) && (ent.norm == ent2.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.start + "\t" + ent.end + "\t" + ent.text + "\t" + ent.norm + "\t" + ent.normId + "\n")
				}
			}

			if (!matched) {
				cur3Annos.each { ent3 ->
					// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
					if ((ent.type == ent3.type) && (ent.norm == ent3.norm) && (!matched)) { // strict_strict
						matched = true
						bf.write(ent.id + "\t" + ent.type + "\t" + ent.start + "\t" + ent.end + "\t" + ent.text + "\t" + ent.norm + "\t" + ent.normId + "\n")
					}
				}
			}

			if (!matched) {
				cur2Annos.each { ent2 ->
					cur3Annos.each { ent3 ->
						if ((ent2.type == ent3.type) && (ent2.norm == ent3.norm)) { // strict_strict
							bf.write(ent2.id + "\t" + ent2.type + "\t" + ent2.start + "\t" + ent2.end + "\t" + ent2.text + "\t" + ent2.norm + "\t" + ent2.normId + "\n")
						}
					}
				}
			}
			
		} else if (cur2Annos.size() > 0) {
			cur2Annos.each { ent2 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent2.type) && (ent.norm == ent2.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.start + "\t" + ent.end + "\t" + ent.text + "\t" + ent.norm + "\t" + ent.normId + "\n")
				}
			}
		} else if (cur3Annos.size() > 0) {
			cur3Annos.each { ent3 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent3.type) && (ent.norm == ent3.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.start + "\t" + ent.end + "\t" + ent.text + "\t" + ent.norm + "\t" + ent.normId + "\n")
				}
			}
		} else {
			bf.write(ent.id + "\t" + ent.type + "\t" + ent.start + "\t" + ent.end + "\t" + ent.text + "\t" + ent.norm + "\t" + ent.normId + "\n")
		}
	}

	// assessing relationships 
	curator1Relations.each { ent ->
		def cur2Annos = curator2Relations.findAll{ ( it.e1start == ent.e1start ) && ( it.e2end == ent.e2end )}
		def cur3Annos = curator3Relations.findAll{ ( it.e1start == ent.e1start ) && ( it.e2end == ent.e2end )}
		boolean matched = false
		
		// needs entity check whether entities have been propagated to GS!
		
		if ((cur2Annos.size() > 0) && (cur3Annos.size() > 0)) {
			cur2Annos.each { ent2 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent2.type) && (ent.norm == ent2.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.e1start + "\t" + ent.e1end + "\t" + ent.e1type + "\t" + ent.e2start + "\t" + ent.e2end + "\t" + ent.e2type + "\n")
				}
			}

			if (!matched) {
				cur3Annos.each { ent3 ->
					// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
					if ((ent.type == ent3.type) && (ent.norm == ent3.norm) && (!matched)) { // strict_strict
						matched = true
						bf.write(ent.id + "\t" + ent.type + "\t" + ent.e1start + "\t" + ent.e1end + "\t" + ent.e1type + "\t" + ent.e2start + "\t" + ent.e2end + "\t" + ent.e2type + "\n")
					}
				}
			}

			if (!matched) {
				cur2Annos.each { ent2 ->
					cur3Annos.each { ent3 ->
						if ((ent2.type == ent3.type) && (ent2.norm == ent3.norm)) { // strict_strict
							bf.write(ent2.id + "\t" + ent2.type + "\t" + ent2.e1start + "\t" + ent2.e1end + "\t" + ent2.e1type + "\t" + ent2.e2start + "\t" + ent2.e2end + "\t" + ent2.e2type + "\n")
						}
					}
				}
			}
		} else if (cur2Annos.size() > 0) {
			cur2Annos.each { ent2 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent2.type) && (ent.norm == ent2.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.e1start + "\t" + ent.e1end + "\t" + ent.e1type + "\t" + ent.e2start + "\t" + ent.e2end + "\t" + ent.e2type + "\n")
				}
			}
		} else if (cur3Annos.size() > 0) {
			cur3Annos.each { ent3 ->
				// !matched to avoid counting match as many times as there are items in the list that cannot be normalised but have the same type
				if ((ent.type == ent3.type) && (ent.norm == ent3.norm) && (!matched)) { // strict_strict
					matched = true
					bf.write(ent.id + "\t" + ent.type + "\t" + ent.e1start + "\t" + ent.e1end + "\t" + ent.e1type + "\t" + ent.e2start + "\t" + ent.e2end + "\t" + ent.e2type + "\n")
				}
			}
		} else {
			bf.write(ent.id + "\t" + ent.type + "\t" + ent.e1start + "\t" + ent.e1end + "\t" + ent.e1type + "\t" + ent.e2start + "\t" + ent.e2end + "\t" + ent.e2type + "\n")
		}
	}

	bf.flush()
	bf.close()
}
