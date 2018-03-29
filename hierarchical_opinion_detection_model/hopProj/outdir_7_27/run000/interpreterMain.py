#coding=utf8

'''
Created on 2017年3月25日

@author: ZhuLixing
'''
import hodmTreeInterpreter

if __name__ == '__main__':
    oHodmTreeInterpreter=hodmTreeInterpreter.hodmTreeInterpreter(
        'iter=001000',
        'iter=001000.assign',
        'BrexitTweets.phrased.phrase',
        'BrexitTweets.phrased',
        50)
    oHodmTreeInterpreter.writehodmtree('hopTree1.txt')
    
    pass
