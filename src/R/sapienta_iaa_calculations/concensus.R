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
#' 
#' 
#' The script expects in its current version the following input parameters:
#' first parameter -- a folder containing containing three subfolders (named "curator1", "curator2", "curator3") 
#'                    and within each folder, an annotation matrix for each publication annotated (file names 
#'                    must match across the three different curators!)
#' second parameter -- an output folder to which the merged annotation matrices will be written
#' 
#' Example of use:
#' 
#' Rscript consensus.R /path/to/curation/matrices /path/to/outputFolder
#' 
#' title: Derivation gold standard from three different annotators assigning multi-label CoreSC annotations
#' author: Shyamasree Saha
#' contributor: James Ravenscroft, Anika Oellrich
#' released: May 2016
#' updated: July 2016
#'--
priority = c(11,4,6,10,2,8,9,3,5,7,1)

readMatrix <- function(path, ann, filename)
{
  mat.df=read.csv(file=paste(path,ann,"/",filename,sep=""),header=TRUE);
  mat.df=mat.df[,-1]
  mat=as.matrix(mat.df);
  mat
}

consensus<-function(f,mat_ann1,mat_ann2,mat_ann3,outpath)
{
  sink(paste("/home/anika/Work/CorpusGeneration/Documents/corpus/results/sapient/analysis/matrices/consensus/log/",f,"log.txt",sep=""))
  if((dim(mat_ann1)[1]!=dim(mat_ann2)[1])||(dim(mat_ann1)[1]!=dim(mat_ann3)[1])||(dim(mat_ann3)[1]!=dim(mat_ann2)[1]))
  {
    print("!!!!!!ERROR!!!!!!")
    #exit();
  }else
  {
    consensus = NULL #matrix(ncol=dim(mat_ann1)[2],dimnames=list(c(),colnames(mat_ann1)))
    
    # annotator with best kappa scores needs to be determined before and set here
    highest_kappa_ann = 3;
    counter = 1
    
    for(i in 1:dim(mat_ann1)[1])
    {
      sent=NULL
      print(paste("sent id:",i,sep=""))
      
      sent=mat_ann1[i,];
      sent=rbind(sent,mat_ann2[i,])
      sent=rbind(sent,mat_ann3[i,])
      print(sent)
      na1=which(mat_ann1[i,]!=0)
      na2=which(mat_ann2[i,]!=0)
      na3=which(mat_ann3[i,]!=0)
      #print(na1)
      #print(na2)
      #print(na3)
      annotated_by = 0
      if(sum(mat_ann1[i,])!=0)
      {
        annotated_by = annotated_by + 1
      }
      if(sum(mat_ann2[i,])!=0)
      {
        annotated_by = annotated_by + 1
      }
      if(sum(mat_ann3[i,])!=0)
      {
        annotated_by = annotated_by + 1
      }
      len = c(length(na1),length(na2),length(na3))
      print(len)
      z=which(len==0)
      if(length(z)==0)
      {
        print("this")
        
        minL=min(len)
      }else
      {
        minL=min(len[-z])
      }
      print("minL")
      print(minL)
      cols=colSums(sent)
      
      mi=which(cols==max(cols))
      maxLab=sum(length(na1),length(na2),length(na3))
      select=c(1:11)*0
      if(sum(sent)==0)
      {
        print("None of them annotated this sentence");
        #select=c(1:11)*0
        consensus=rbind(consensus,select[1:11])
      }
      else
      {
        if(length(mi)>=maxLab)
        {
          print("no match, sent id and sent");
          #print(i)
          
          if(length(mi)==annotated_by) ##this means all of them assigned 1 label
          {
            print("no match, 1 label");
            #select=c(1:11)*0
            select[which(sent[highest_kappa_ann,]!=0)]=1;
            #print(i)
            print(select)
            #consensus=rbind(consensus,select[1:11])
          }
          else
          {
            print("more than 1 label"); ###need to decide, but never happend
          }
        }
        else
        {
          #select=c(1:11)*0
          copyCols = cols
          colus=unique(sort(copyCols,decreasing=TRUE))
          print("everybody agreed for  on at least one label,colus")
          print(colus)
          print("cols")
          print(cols)
          print(colus[1])
          #select[,mi]=max(cols)
          
          #else
          #{
          for(ind in 1:minL)
          {
            
            if(minL==1)
            {
              
              
              mInd=which(cols==colus[ind])
              
              if(length(mInd)==1)
              {
                select[mInd]=1;
              }
              else
              {
                print (priority)
                print (mInd)
                print (unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                print("many max, minL 1")
                select[priority[min(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))]]=1;
                
              }
            }
            
            
            if(minL==2)
            {
              mInd=which(cols==colus[ind])
              if(length(mInd)==1)
              {
                if(ind==1)
                {
                  select[which(cols==colus[ind])]=0.6;
                }
                else
                {
                  select[which(cols==colus[ind])]=0.4;
                }
              }
              else
              {
                print("many max minL 2")
                if(ind==1)
                {
                  ##select[which(cols==colus[ind])]=0.6;
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[priority[mnd[1]]]=0.6;
                  select[priority[mnd[2]]]=0.4;
                  print("sc1 and 2 both assigned")
                  ind=4 # I assign 4 as we have maximum 3 label
                  break;
                }
                else
                {
                  print("should not come here is sc1 and 2 was printed")
                  if(ind==2)
                  {
                    print("should not come here is sc1,2 and 3 was printed")
                    mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                    select[mnd[1]]=0.4;
                    
                    ind=4 # same reason as before
                    break;
                  }
                }
              }
            }
            if(minL==3)
            {
              mInd=which(cols==colus[ind])
              if(length(mInd)==1)
              {
                if(ind==1)
                {
                  select[which(cols==colus[ind])]=0.6;
                }
                else
                {
                  if(ind==2)
                  {
                    select[which(cols==colus[ind])]=0.3;
                  }
                  else
                  {
                    select[which(cols==colus[ind])]=0.1;
                  }
                }
              }
              else
              {
                print("many max minL 3")
                if(ind==1)
                {
                  ##select[which(cols==colus[ind])]=0.6;
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[priority[mnd[1]]]=0.6;
                  select[priority[mnd[2]]]=0.3;
                  if(length(mnd)==3)
                  {
                    select[priority[mnd[3]]]=0.3;
                    print("sc1, 2 and 3, all assigned")
                    break;
                  }
                  else
                  {
                    mInd=which(cols==colus[ind+1])
                    if(length(mInd)==1)
                    {
                      select[mInd]=0.1;
                    }
                    else
                    {
                      mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                      select[priority[mnd[1]]]=0.1;
                    }
                    ind=4 # same reason as before
                    break;
                  }
                }
                if(ind==2)
                {
                  print("should not come here is sc1,2 and 3 was printed")
                  mnd=sort(unlist(lapply(mInd,function(X){ which( priority %in% X ) } )))
                  select[mnd[1]]=0.3;
                  select[mnd[2]]=0.1;
                  ind=4 # same reason as before
                  break;
                }
                if(ind==3)
                {
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
        #}
        
        #print(i)
        print(select)
        consensus=rbind(consensus,c(counter,select[1:11]))
      }
      counter = counter + 1
    }##end of forR
    colnames(consensus) = c("sid",colnames(mat_ann1))
    write.csv(consensus,file=paste(outpath,f,".txt",sep=""),row.names=FALSE, quote=FALSE)
  }
  sink(NULL);
}

main<-function()
{
  path = "/home/anika/Work/CorpusGeneration/Documents/corpus/results/sapient/analysis/matrices/paperMatrices/";
  print(path)
  outpath = "/home/anika/Work/CorpusGeneration/Documents/corpus/results/sapient/analysis/matrices/consensus/";
  files=list.files(path = paste(path,"curator1/",sep=""))
  #files=gsub("_arathi","",files);
  for(f in files)
  {
    ann1="curator1"
    ann2="curator2"
    ann3="curator3"
    
    mat_ann1=readMatrix(path,ann1,f)
    mat_ann2=readMatrix(path,ann2,f)
    mat_ann3=readMatrix(path,ann3,f)
    consensus(f,mat_ann1,mat_ann2,mat_ann3,outpath)
  }
}
