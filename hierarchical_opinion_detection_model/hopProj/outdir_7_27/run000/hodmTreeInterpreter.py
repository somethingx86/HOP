#coding=utf8

'''
Created on 2017年3月25日

@author: ZhuLixing
'''

import sys, re, os, itertools, math
import operator
from lib2to3.pytree import Node

class hodmTreeInterpreter:    
    '''
    classdocs
    A class to convert the GibbsOutput into the tree
    It can also be modified to meet the special needs if we read in the tree
    '''
    
    __state=None    #__state是一个大字典,里面存有docdict,tree两个变量
    __vocalst=None     #__vocalst是一个列表,它的下标就是从数字到voca的映射
    __lstdocmap=None    #__lstdocmap是一个列表,它的下标是从数字到doc内容的映射
    
    def __init__(self, iterFilename,iterAssignFilename,vocabFilename,docmapFilename,topNWordsToRepresentTopic):
        '''
        Constructor
        vocabFilename is the indexed vocabulary of corpus, starting from 0
        docmapFilename is the indexed document of corpus, starting from 0
        topicNWordsToRepresentTopic is the number of words to represent the topic
        
        the __state __vocalst and __lstdocmap is assigned
        
        '''
        
        self.__vocalst=list(map(str.strip,open(vocabFilename, "r",encoding='utf8').readlines()))
        #vocab是一个list,由于vocab_filename本身的数据格式,这个list的下标正好是每个vocab对应的id号
        #str.strip是一个函数,python的map跟别的语言的map可不一样,python的map是指对每个元素进行一个操作
        #strip这个函数就是要求去掉首位的空白符号
        self.__readState(iterFilename, iterAssignFilename,topNWordsToRepresentTopic)
        self.__hodmTreeAddDocumentAssignments(self.__state['hodmtree'], iterAssignFilename)#有用,这样hodmtree中每个topic就关联上了一串docid
        #self.__readLstdocmap(docmapFilename)#事实上,这个根本用不到
        #因此,我们就不要管我生成的ldadoc对不对了,因为根本不会用到..关于具体是处理后的文档按原文档文字顺序去掉停用词后输出还是直接把涉及的单词输出，暂时不去研究|
        
    
    def __readState(self,iterFilename,iterAssignFilename,topNWordsToRepresentTopic):
        '''
        Read the state
        the __vocalst must have been assigned
        
        the __state will be assigned
        '''
        stateFile=open(iterFilename, 'r',encoding='utf8')
        
        score         = float(stateFile.readline().split()[1]) #score,暂时没用
        iternum          = int(stateFile.readline().split()[1])    #迭代次数
        eta           = stateFile.readline().split()            #每层的eta,是个向量
        eta           = [float(x) for x in eta[1:len(eta)]]
        gam           = stateFile.readline().split()            #从第二层开始每层的gamma,是个向量
        gam           = [float(x) for x in gam[1:len(gam)]]     
        gem_mean      = float(stateFile.readline().split()[1])
        gem_scale     = float(stateFile.readline().split()[1])
        scaling_shape = float(stateFile.readline().split()[1])
        scaling_scale = float(stateFile.readline().split()[1])

        sectionTitle = stateFile.readline() #每一段的title,暂时没用
        
        hodmtree=dict() #字典,其实是一个id到topic的映射,但是,它每个topic中又有个list children,每个成员是一个id
    
        for line in stateFile:                          #可以直接对file用的
            (topicId, parent, ndocs, nwords, scale, word_cnt) = line.split(None, 5)#None表示使用默认的限定符,5表示分隔符从左到右使用5次,一上来不算
            (topicId, parent, ndocs, nwords) = [int(x) for
                                           x in [topicId, parent, ndocs, nwords]] #str转int,然后存到了list里,然后又被强制转换为tuple
            scale = float(scale)    #就是每层的gamma,没什么用
            hodmtree[topicId] = dict()  #topicId的key指向一个topic结构,其实也是一个字典
            hodmtree[topicId]['parent'] = parent    #parent肯定已经被读进去了,因为当时输出的时候是按照DFS输出的
            if (parent >= 0): #是孩子结点
                hodmtree[parent]['children'].append(topicId)
    
            hodmtree[topicId]['nwords'] = nwords
            hodmtree[topicId]['ndocs'] = ndocs
            hodmtree[topicId]['scale'] = scale
            lstTopicWords = [int(x) for x in word_cnt.split()]  #就是临时存储的一个变量,存的是一个topic在每个vocab下的单词数量
            hodmtree[topicId]['top_words'] = self.__topNWords(lstTopicWords, topNWordsToRepresentTopic)   #这边是words排序的
            #注意了,经过这一步处理,实际上这个topic结构里面并没有记录每个单词的数量
            hodmtree[topicId]['children'] = list()  #hodmtree就是一个dict,一个topic的children是一个list        
        
        self.__state={'score':score,
            'iter':iternum,
            'gam':gam,
            'eta':eta,
            'gem_mean':gem_mean,
            'gem_scale':gem_scale,
            'scaling_shape':scaling_shape,
            'scaling_scale':scaling_scale,
            'hodmtree':hodmtree}
        
    def __topNWords(self,lstTopicWords,topNWordsToRepresentTopic):
        '''
        topicWords: a list of word counts of a topic indexed in the vocab
        return the top N words string, that the top N words is in a list, each element is a vocab string
        
        __vocalst is just a list, whose index is mapped into a vocab
        '''    
        
        lstVocabCount=list()    #这样一个list,它的每个元素是一个元组,左边是vocab号,右边是这个vocab出现的次数
        for i in range(0,len(self.__vocalst)):
            lstVocabCount.append(tuple((i,lstTopicWords[i])))
        getitem1=operator.itemgetter(1) #按照list里面的第二个元素排序
        lstVocabCount.sort(key=getitem1,reverse=True)
        lstCollum0OfLstVocabCount=list()
        for i in range(0,len(self.__vocalst)):
            lstCollum0OfLstVocabCount.append(lstVocabCount[i][0])
        #lstCollum0OfLstVocabCount其实是一个list,是lstVocabCount的第一列,它从前往后元素实际上是排名前几的VocaIndex
        lstTopNWords=[self.__vocalst[i] for i in lstCollum0OfLstVocabCount[0:topNWordsToRepresentTopic]]
        #把vocaIndex映射到真实的word,word在这里是string类型
        return lstTopNWords
        
    def __hodmTreeAddDocumentAssignments(self,hodmtree,iterAssignFilename):
        '''
        each tree topic is add with a list called docs, whose elements are doc ids associated with this topic
        because hodmtree is a dict so we can change the values in it
        '''
        for line in open(iterAssignFilename, 'r',encoding='utf8'):
            (doc_id, score, path) = line.split(None, 2)
            doc_id = int(doc_id)
            score = float(score)
            path = [int(x) for x in path.split()]
            for topicId in path:
                hodmtree[topicId].setdefault('docs', list())   #这个键如果不存在将会被添加,指向一个空的list,等价于 hodmtree[topicId]['docs']=list()
                hodmtree[topicId]['docs'].append((doc_id, score)) #list的每个元素是一个doc_id和score,score就是Gibbs Sampling的Score吧
                
    def __readLstdocmap(self,docmapFilename):
        """
        read a doc-map, which is assumed to be a list of titles
    
        self.__lstdocmap is assigned, whose index is pointing to a doc content
        """
        self.__lstdocmap = list()   #一个空的list,[]表示一个空的list,()表示一个空的tuple
        for line in open(docmapFilename, 'r',encoding='utf8'):
            self.__lstdocmap.append(line)#注意 原来是 {'title':line} 现被改为line\
            
    def writehodmtree(self,outFilename,mindocs=0):
        '''
        outFilename is the output of the tree
        mindocs is the lowerbound document counts for a topic to output
        '''
        
        out = open(outFilename, 'w',encoding='utf8')
        hodmtree = self.__state['hodmtree']
    
        eta = ' '.join(['%1.3e' % x for x in self.__state['eta']])
        gam = ' '.join(['%1.3e' % x for x in self.__state['gam']])
    
        out.write('SCORE         = %s\n' % str(self.__state['score']))
        out.write('ITER          = %s\n' % str(self.__state['iter']))
        out.write('ETA           = %s\n' % eta)
        out.write('GAM           = %s\n' % gam)
        out.write('GEM_MEAN      = %s\n' % str(self.__state['gem_mean']))
        out.write('GEM_SCALE     = %s\n' % str(self.__state['gem_scale']))
        out.write('SCALING_SHAPE = %s\n' % str(self.__state['scaling_shape']))
        out.write('SCALING_SCALE = %s\n\n' % str(self.__state['scaling_scale']))
    
        max_level = len(self.__state['gam'])    #注意了,这里是按照gammma的数量判断深度的,max_level=depth-1,而len(gam)=depth-1
    
        def write_topic(topic, level,currTopicId):
    
            indent = '     ' * level#空的格数,目前的设置是每层空5格
            out.write('%s' % indent)
            out.write("[%d/%d/%d/%d]" % (level, currTopicId,topic['nwords'], topic['ndocs']))#应该修改,最前面是topicId
            # out.write(' %s' % str(topic['scale']))
            out.write(' %s\n\n' % ' '.join([x.upper() for x in topic['top_words']]))
    
            if (level == max_level):
                #树叶(末层)后面是要多输出个 
                out.write('\n')
            for childTopicId in topic['children']:
                if ((hodmtree[childTopicId]['ndocs'] >= mindocs) and
                    (hodmtree[childTopicId]['nwords'] > 0)):#大于一定量的依附的word才予以输出
                    write_topic(hodmtree[childTopicId], level + 1,childTopicId)#这样的话id也必须传进去,虽然原本是作为字典的key存储的
    
        write_topic(hodmtree[0], 0,0)
        out.close()
        
                