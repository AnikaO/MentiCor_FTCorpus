#' Copyright (c) <2016>, <Shyamasree Saha>
#' All rights reserved.
#' Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions 
#' are met:
#' 
#' 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#' 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
#'    in the documentation and/or other materials provided with the distribution.
#' 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
#'    derived from this software without specific prior written permission.
#'    
#' THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
#' LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#' HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#' LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#' THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
#' OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#'--
#' This scripts merges structure annotations (CoreSCs) based on the annotations assigned by three different curators individually. 
#' The merging process takes into consideration the curator who has been identified as the most reliable curator using the Inter-annotator
#' agreement measures (calculated with kappa_calculations.R; the curator that has consistently higher agreements with the other two curators).
#' In addition, it also follows the predefined priorities of the CoreSC concepts (see ), which aims to propagate those annotations with the
#' highest priorities to the gold standard.
#' 
#' The script expects in its current version the following input parameters:
#' first parameter -- a folder containing containing three subfolders (named "curator1", "curator2", "curator3") 
#'                    and within each folder, an annotation matrix for each publication annotated (file names 
#'                    must match across the three different curators!)
#' second parameter -- an output folder to which the merged annotation matrices will be written
#' third parameter -- the number of the curator deemed most reliable based on IAA scores 
#' 
#' Example of use:
#' 
#' Rscript consensus.R /path/to/curation/matrices /path/to/outputFolder 3
#' 
#'--
#' title: Derivation gold standard from three different annotators assigning multi-label CoreSC annotations
#' author: Shyamasree Saha
#' contributors: James Ravenscroft, Anika Oellrich
#' released: May 2016
#' updated: July 2016
#'--

#'--
#' Read the annotation matrix as assigned by one curator only. Each file contains a matrix consisting of 12 columns,
#' one for the sentence identifier and the remaining eleven covering the CoreSC concepts used for annotation. The 
#' values in each of the columns can range from 0 to 1, depending on how many annotations have been assigned to a 
#' sentence and in which order. The column ordering must be consistent across all the publications annotated and 
#' across all curators.
#' 
#' @param path path to the directory containing annotation matrices for all three curators
#' @param ann specifies the curator ("curator1", "curator2" or "curator3" here)
#' @param filename defines the specific publication for which annotations have been assigned (here PMC identifiers;
#'                 contains the actual matrix)
#' @return Read Annotation matrix for a specific curator and file (here publication)
#'--
readMatrix <- function(path, ann, filename) {
  mat.df=read.csv(file=paste(path,ann,"/",filename,sep=""),header=TRUE);
  mat.df=mat.df[,-1]
  mat=as.matrix(mat.df);
  return(mat)
}

#'--
#' Goes through the individually assigned annotations by all three curators and determines which to propagate to 
#' the gold standard based on the priority of the annotation and the most reliable annotator.
#' 
#' @param f file (here publication) that is currently processed
#' @param mat_ann1 annotations assigned by curator one
#' @param mat_ann2 annotations assigned by curator two
#' @param mat_ann3 annotations assigned by curator three
#' @param ref_ann number of the annotator that is deemed most reliable based on IAA scores
#' @param output folder where gold standard should be written to
#'--
consensus<-function(f,mat_ann1,mat_ann2,mat_ann3,ref_ann,output) {
  ## open log file to comment on progress
  sink(paste(output,"log/",f,"_log.txt",sep=""))
  
  ## sizes of annotation matrices need to match, i.e. curators MUST have annotated the same number of sentences
  if((dim(mat_ann1)[1]!=dim(mat_ann2)[1])||(dim(mat_ann1)[1]!=dim(mat_ann3)[1])||(dim(mat_ann3)[1]!=dim(mat_ann2)[1])) {
    print("Error in function consensus -- Dimensions of matrices for individual curators do not match")
  } else {
    consensus = NULL
    
    ## set curator with highest reliability here for further computation
    highest_kappa_ann = as.numeric(ref_ann)
    counter = 1
    
    ## go through all the sentences in the file individually
    for(i in 1:dim(mat_ann1)[1])
    {
      sent=NULL
      print(paste("sent id:",i,sep=""))
      
      ## read annotations assigned by three curators for this sentence
      sent=mat_ann1[i,];
      sent=rbind(sent,mat_ann2[i,])
      sent=rbind(sent,mat_ann3[i,])
      
      na1=which(mat_ann1[i,]!=0)
      na2=which(mat_ann2[i,]!=0)
      na3=which(mat_ann3[i,]!=0)
      
      ## determine how many curators have assigned at least one annotation to this sentence
      annotated_by = 0
      if(sum(mat_ann1[i,])!=0) {
        annotated_by = annotated_by + 1
      }
      
      if(sum(mat_ann2[i,])!=0) {
        annotated_by = annotated_by + 1
      }
      
      if(sum(mat_ann3[i,])!=0) {
        annotated_by = annotated_by + 1
      }
      
      ## determine the least number of annotations assigned to sentence
      len = c(length(na1),length(na2),length(na3))
      z=which(len==0)
      
      if(length(z)==0) { # if all annotators assigned at least one annotation
        print("Each annotator assigned at least one annotation")
        minL=min(len) # set to minimum number of annotations
      } else {
        minL=min(len[-z]) # minimum number of annotations, potentially >0; 0 if no annotator assigned any annotation
      }
      
      print(paste("Minimum annotations assigned: ", minL, sep=""))
      
      ## determines which annotation(s) was assigned most often by all three curators
      cols=colSums(sent)
      mi=which(cols==max(cols))
      ## calculate the total number of annotations assigned by all three curators
      maxLab=sum(length(na1),length(na2),length(na3))
      select=c(1:11)*0 # initialise vector for annotation results
      print(paste("Total number of annotations assigned: ", maxLab, sep=""))
      
      if(sum(sent)==0) {
        print("No curator assigned any annotations to this sentence");
        consensus=rbind(consensus,select[1:11]) # no annotations to be propagated to gold standard, so sentence include but without any annotations
      } else {
        if(length(mi)>=maxLab) { # > can never happen
          if(length(mi)==annotated_by) { # all annotators that assigned annotations do not agree on annotation
            select[which(sent[highest_kappa_ann,]!=0)]=1; 
            print(paste("Each curator assigned one annotation, but none matched. Selecting annotation from most reliable curator: ",select, sep="")) ## where is "no match" coming from?!
          } else {
            print("This case should never happen!")
          }
        } else {
          colus=unique(sort(cols,decreasing=TRUE))
          print("Some of the curators agreed on at least one label -- colus: ")
          print(colus)
          print("Summary across all three curators -- cols")
          print(cols)
          
          for(ind in 1:minL) {
            if (minL==1) {
              mInd=which(cols==colus[ind])
              
              if(length(mInd)==1) {
                select[mInd]=1;
              } else {
                print (priority)
                print (mInd)
                print (unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                print("many max, minL 1")
                select[priority[min(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))]]=1;
              }
            }
            
            if(minL==2) {
              mInd=which(cols==colus[ind])
              if(length(mInd)==1) {
                if(ind==1) {
                  select[which(cols==colus[ind])]=0.6;
                } else {
                  select[which(cols==colus[ind])]=0.4;
                }
              } else {
                print("many max minL 2")
                if(ind==1) {
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[priority[mnd[1]]]=0.6;
                  select[priority[mnd[2]]]=0.4;
                  print("sc1 and 2 both assigned")
                  ind=4 # I assign 4 as we have maximum 3 label
                  break;
                } else {
                  print("should not come here is sc1 and 2 was printed")
                  if(ind==2) {
                    print("should not come here is sc1,2 and 3 was printed")
                    mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                    select[mnd[1]]=0.4;
                    ind=4 # same reason as before
                    break;
                  }
                }
              }
            }
            if(minL==3) {
              mInd=which(cols==colus[ind])
              if(length(mInd)==1) {
                if(ind==1) {
                  select[which(cols==colus[ind])]=0.6;
                } else {
                  if(ind==2) {
                    select[which(cols==colus[ind])]=0.3;
                  } else {
                    select[which(cols==colus[ind])]=0.1;
                  }
                }
              } else {
                print("many max minL 3")
                if(ind==1)
                {
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[priority[mnd[1]]]=0.6;
                  select[priority[mnd[2]]]=0.3;
                  if(length(mnd)==3) {
                    select[priority[mnd[3]]]=0.3;
                    print("sc1, 2 and 3, all assigned")
                    break;
                  } else {
                    mInd=which(cols==colus[ind+1])
                    if(length(mInd)==1) {
                      select[mInd]=0.1;
                    } else {
                      mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                      select[priority[mnd[1]]]=0.1;
                    }
                    ind=4 # same reason as before
                    break;
                  }
                }
                if(ind==2) {
                  print("should not come here is sc1,2 and 3 was printed")
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[mnd[1]]=0.3;
                  select[mnd[2]]=0.1;
                  ind=4 # same reason as before
                  break;
                }
                if(ind==3) {
                  print("should not come here is sc1,2 and 3 was printed")
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[mnd[1]]=0.1;
                  ind=4 # same reason as before
                  break;
                }
              }
            }
          }
        }
        print(select)
        consensus=rbind(consensus,c(counter,select[1:11]))
      }
      counter = counter + 1
    }
    
    colnames(consensus) = c("sid",colnames(mat_ann1))
    write.csv(consensus,file=paste(output,f,".txt",sep=""),row.names=FALSE, quote=FALSE)
  }
  sink(NULL);
}

## pre-configuring the concept priorities; means that curator matrices MUST have specific column arrangement!
## column order: Bac, Con, Exp, Goa, Met, Mot, Obs, Res, Mod, Obj, Hyp
priority = c(11,4,6,10,2,8,9,3,5,7,1)

## read command line parameters for input and output folder, and the most reliable curator
args<-commandArgs(TRUE)
input = args[1] # folder containing the annotations of all three curators in individual folders
output = args[2]
ref_ann = args[3]

## names of folders specific to the three annotators, i.e. folders in input
ann1="curator1"
ann2="curator2"
ann3="curator3"

## read the names of all the files that have been annotated (here publications); MUST be named identically across 
## all curators
files=list.files(path = paste(input,ann1,sep=""))

for(f in files)
{
  print(f)  
  ## read annotations assigned by three curators
  mat_ann1=readMatrix(input,ann1,f)
  mat_ann2=readMatrix(input,ann2,f)
  mat_ann3=readMatrix(input,ann3,f)
  
  ## determine those that need propagation and write gold standard to output folder
  consensus(f,mat_ann1,mat_ann2,mat_ann3,ref_ann,output)
}
